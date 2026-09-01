import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { getPets } from "../../pet/api/petApi";
import type { Pet } from "../../pet/types/pet";
import {
    getPlaceCompatibility,
    searchAllPlaces,
    type CompatibilityStatus,
    type PlaceCompatibility,
    type PlaceSummary,
} from "../../place/api/placeApi";
import KakaoMap from "../../place/components/KakaoMap";
import type { MapCenter, MapMarker } from "../../place/types/map";

import "../styles/travel-course.css";

const SEOUL_CENTER: MapCenter = { latitude: 37.5665, longitude: 126.978 };
const STORAGE_KEY = "petway-travel-course";

const REGIONS = [
    { code: "1", name: "서울특별시", center: SEOUL_CENTER },
    { code: "2", name: "인천광역시", center: { latitude: 37.4563, longitude: 126.7052 } },
    { code: "3", name: "대전광역시", center: { latitude: 36.3504, longitude: 127.3845 } },
    { code: "4", name: "대구광역시", center: { latitude: 35.8714, longitude: 128.6014 } },
    { code: "5", name: "광주광역시", center: { latitude: 35.1595, longitude: 126.8526 } },
    { code: "6", name: "부산광역시", center: { latitude: 35.1796, longitude: 129.0756 } },
    { code: "7", name: "울산광역시", center: { latitude: 35.5384, longitude: 129.3114 } },
    { code: "8", name: "세종특별자치시", center: { latitude: 36.48, longitude: 127.289 } },
    { code: "31", name: "경기도", center: { latitude: 37.4138, longitude: 127.5183 } },
    { code: "32", name: "강원특별자치도", center: { latitude: 37.8228, longitude: 128.1555 } },
    { code: "33", name: "충청북도", center: { latitude: 36.6357, longitude: 127.4917 } },
    { code: "34", name: "충청남도", center: { latitude: 36.6588, longitude: 126.6728 } },
    { code: "35", name: "경상북도", center: { latitude: 36.4919, longitude: 128.8889 } },
    { code: "36", name: "경상남도", center: { latitude: 35.4606, longitude: 128.2132 } },
    { code: "37", name: "전북특별자치도", center: { latitude: 35.7175, longitude: 127.153 } },
    { code: "38", name: "전라남도", center: { latitude: 34.8679, longitude: 126.991 } },
    { code: "39", name: "제주특별자치도", center: { latitude: 33.4996, longitude: 126.5312 } },
] as const;

const TYPE_LABELS: Record<string, string> = {
    "12": "관광지", "14": "문화시설", "15": "축제·행사", "28": "레포츠",
    "32": "숙박", "38": "쇼핑", "39": "음식점",
};

const STAY_MINUTES: Record<string, number> = {
    "12": 90, "14": 90, "15": 120, "28": 120, "32": 60, "38": 60, "39": 75,
};

type CourseStyle = "RELAXED" | "BALANCED" | "ACTIVE";

interface Recommendation extends PlaceSummary {
    compatibility?: PlaceCompatibility;
}

function distanceKm(left: MapCenter, right: MapCenter): number {
    const radians = (degree: number) => degree * Math.PI / 180;
    const latitudeDelta = radians(right.latitude - left.latitude);
    const longitudeDelta = radians(right.longitude - left.longitude);
    const value = Math.sin(latitudeDelta / 2) ** 2
        + Math.cos(radians(left.latitude)) * Math.cos(radians(right.latitude))
        * Math.sin(longitudeDelta / 2) ** 2;
    return 6371 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
}

function travelMinutes(left: PlaceSummary, right: PlaceSummary): number | null {
    if (left.latitude === null || left.longitude === null || right.latitude === null || right.longitude === null) return null;
    const distance = distanceKm(
        { latitude: left.latitude, longitude: left.longitude },
        { latitude: right.latitude, longitude: right.longitude },
    );
    return Math.max(5, Math.round((distance / 30) * 60 + 8));
}

function formatDistance(origin: MapCenter, place: PlaceSummary): string {
    if (place.latitude === null || place.longitude === null) return "거리 확인 필요";
    const distance = distanceKm(origin, { latitude: place.latitude, longitude: place.longitude });
    return distance < 1 ? `${Math.round(distance * 1000)}m` : `${distance.toFixed(1)}km`;
}

function statusCopy(status?: CompatibilityStatus) {
    if (status === "AVAILABLE") return { label: "함께 갈 수 있어요", className: "available" };
    if (status === "UNAVAILABLE") return { label: "이용이 어려워요", className: "unavailable" };
    return { label: "방문 전 확인해 주세요", className: "confirmation" };
}

export default function TravelCoursePage() {
    const [pets, setPets] = useState<Pet[]>([]);
    const [regionCode, setRegionCode] = useState("1");
    const [tripDays, setTripDays] = useState(2);
    const [selectedDay, setSelectedDay] = useState(0);
    const [origin, setOrigin] = useState<MapCenter>(SEOUL_CENTER);
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [regionPlaceCount, setRegionPlaceCount] = useState(0);
    const [courses, setCourses] = useState<Recommendation[][]>(() => {
        try {
            const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "[]") as Recommendation[] | Recommendation[][];
            return Array.isArray(saved[0]) ? saved as Recommendation[][] : [saved as Recommendation[], []];
        } catch { return [[], []]; }
    });
    const [style, setStyle] = useState<CourseStyle>("BALANCED");
    const [selectedType, setSelectedType] = useState("all");
    const [startTime, setStartTime] = useState("10:00");
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("서울 중심으로 추천 장소를 준비하고 있어요.");

    const primaryPet = pets.find((pet) => pet.isPrimary) ?? pets[0] ?? null;
    const maxStops = style === "RELAXED" ? 3 : style === "ACTIVE" ? 6 : 4;
    const course = courses[selectedDay] ?? [];
    const selectedRegion = REGIONS.find((region) => region.code === regionCode) ?? REGIONS[0];

    const updateCourse = (updater: Recommendation[] | ((current: Recommendation[]) => Recommendation[])) => {
        setCourses((current) => {
            const next = Array.from({ length: tripDays }, (_, index) => current[index] ?? []);
            next[selectedDay] = typeof updater === "function" ? updater(next[selectedDay]) : updater;
            return next;
        });
    };

    useEffect(() => {
        getPets().then(setPets).catch(() => setPets([]));
    }, []);

    const loadRecommendations = async (areaCode: string, center: MapCenter, locationLabel: string) => {
        setLoading(true);
        setRecommendations([]);
        setRegionPlaceCount(0);
        setMessage(`${locationLabel} 주변에서 반려견 동반 장소를 찾고 있어요.`);
        try {
            const response = await searchAllPlaces(areaCode);
            setRegionPlaceCount(response.items.length);
            const nearby = response.items
                .filter((place) => place.latitude !== null && place.longitude !== null)
                .sort((left, right) => {
                    const leftDistance = distanceKm(center, { latitude: left.latitude!, longitude: left.longitude! });
                    const rightDistance = distanceKm(center, { latitude: right.latitude!, longitude: right.longitude! });
                    return leftDistance - rightDistance;
                });

            const assessed: Recommendation[] = [];
            const compatibilityBatchSize = 6;
            for (let index = 0; index < nearby.length; index += compatibilityBatchSize) {
                const batch = nearby.slice(index, index + compatibilityBatchSize);
                const batchResult = await Promise.all(batch.map(async (place): Promise<Recommendation> => {
                    try { return { ...place, compatibility: await getPlaceCompatibility(place.contentId) }; }
                    catch { return { ...place, compatibility: undefined }; }
                }));
                assessed.push(...batchResult);
                const eligibleSoFar = assessed.filter((place) => place.compatibility?.petwayAssessment.status !== "UNAVAILABLE");
                setRecommendations(eligibleSoFar);
                setMessage(`${locationLabel} 전체 ${nearby.length}곳 중 ${Math.min(index + compatibilityBatchSize, nearby.length)}곳의 동반 조건을 확인했어요.`);
            }
            const eligible = assessed.filter((place) => place.compatibility?.petwayAssessment.status !== "UNAVAILABLE");
            setRecommendations(eligible);
            setMessage(`${locationLabel} 전체 ${response.items.length}곳을 확인해 ${eligible.length}곳을 추천했어요.`);
        } catch (error) {
            setRecommendations([]);
            setMessage(error instanceof Error ? error.message : "추천 장소를 불러오지 못했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { void loadRecommendations("1", SEOUL_CENTER, "서울특별시"); }, []);
    useEffect(() => { localStorage.setItem(STORAGE_KEY, JSON.stringify(courses)); }, [courses]);

    useEffect(() => {
        setCourses((current) => Array.from({ length: tripDays }, (_, index) => current[index] ?? []));
        setSelectedDay((current) => Math.min(current, tripDays - 1));
    }, [tripDays]);

    const filteredRecommendations = useMemo(() => recommendations.filter((place) =>
        selectedType === "all" || place.contentTypeId === selectedType
    ), [recommendations, selectedType]);

    const availableTypes = useMemo(() => Array.from(new Set(recommendations.map((place) => place.contentTypeId))), [recommendations]);

    const routeInfo = useMemo(() => course.map((place, index) => ({
        place,
        travel: index === 0 ? null : travelMinutes(course[index - 1], place),
        stay: STAY_MINUTES[place.contentTypeId] ?? 75,
    })), [course]);

    const totalMinutes = routeInfo.reduce((sum, item) => sum + item.stay + (item.travel ?? 0), 0);
    const finishTime = useMemo(() => {
        const [hour, minute] = startTime.split(":").map(Number);
        const value = hour * 60 + minute + totalMinutes;
        return `${String(Math.floor(value / 60) % 24).padStart(2, "0")}:${String(value % 60).padStart(2, "0")}`;
    }, [startTime, totalMinutes]);

    const markers = useMemo<MapMarker[]>(() => course.map((place, index) => ({
        id: place.contentId,
        title: `${index + 1}. ${place.title}`,
        latitude: place.latitude!,
        longitude: place.longitude!,
        kind: "place",
    })), [course]);

    const addPlace = (place: Recommendation) => {
        if (course.some((item) => item.contentId === place.contentId) || course.length >= maxStops) return;
        updateCourse((current) => [...current, place]);
    };

    const moveCourseItem = (index: number, direction: -1 | 1) => {
        const target = index + direction;
        if (target < 0 || target >= course.length) return;
        updateCourse((current) => {
            const next = [...current];
            [next[index], next[target]] = [next[target], next[index]];
            return next;
        });
    };

    const changeRegion = (code: string) => {
        const region = REGIONS.find((item) => item.code === code) ?? REGIONS[0];
        setRegionCode(code);
        setOrigin(region.center);
        setSelectedType("all");
        void loadRecommendations(region.code, region.center, region.name);
    };

    return (
        <main className="course-page">
            <div className="course-page-inner">
                <header className="course-hero">
                    <div>
                        <span className="course-eyebrow">MY DAY TRIP</span>
                        <h1>우리 아이와 보내는 하루를<br />가볍게 설계해 보세요</h1>
                        <p>큰 틀만 선택하면 갈 수 있는 장소를 확인하고, 원하는 곳을 자유롭게 더할 수 있어요.</p>
                    </div>
                    <div className="course-trip-badge"><strong>{selectedRegion.name}</strong><span>{tripDays - 1}박 {tripDays}일</span></div>
                </header>

                <section className="course-settings" aria-label="여행 기본 설정">
                    <label className="course-select-field">여행 지역
                        <select value={regionCode} onChange={(event) => changeRegion(event.target.value)}>
                            {REGIONS.map((region) => <option key={region.code} value={region.code}>{region.name}</option>)}
                        </select>
                    </label>
                    <label className="course-select-field">여행 기간
                        <select value={tripDays} onChange={(event) => setTripDays(Number(event.target.value))}>
                            {[2, 3, 4, 5, 6, 7].map((days) => <option key={days} value={days}>{days - 1}박 {days}일</option>)}
                        </select>
                    </label>
                    <div className="course-pet-summary">
                        <span>함께 갈 반려견</span>
                        {primaryPet ? <strong>{primaryPet.petName}</strong> : <Link to="/pets/new">반려견 등록하기</Link>}
                        <small>{primaryPet ? `${primaryPet.weightKg}kg · ${primaryPet.sizeType ?? "체형 확인 필요"}` : "등록하면 장소별 동반 가능 여부를 확인해 드려요."}</small>
                    </div>
                    <fieldset>
                        <legend>여행 템포</legend>
                        {(["RELAXED", "BALANCED", "ACTIVE"] as CourseStyle[]).map((value) => (
                            <button key={value} type="button" className={style === value ? "active" : ""} onClick={() => setStyle(value)}>
                                {value === "RELAXED" ? "느긋하게" : value === "ACTIVE" ? "알차게" : "적당하게"}
                            </button>
                        ))}
                    </fieldset>
                    <label className="course-time-field">출발 시간<input type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} /></label>
                </section>

                <p className="course-status" role="status">{message}</p>

                <div className="course-workspace">
                    <section className="course-recommendations">
                        <header><div><span>RECOMMENDATIONS</span><h2>{primaryPet ? `${primaryPet.petName}와 갈 만한 곳` : "지역 추천 장소"}</h2></div><strong>{filteredRecommendations.length} / 전체 {regionPlaceCount}곳</strong></header>
                        <div className="course-type-filters">
                            <button type="button" className={selectedType === "all" ? "active" : ""} onClick={() => setSelectedType("all")}>전체</button>
                            {availableTypes.map((type) => <button type="button" key={type} className={selectedType === type ? "active" : ""} onClick={() => setSelectedType(type)}>{TYPE_LABELS[type] ?? "기타"}</button>)}
                        </div>
                        <div className="course-card-list">
                            {loading && <div className="course-empty">장소를 확인하고 있어요.</div>}
                            {!loading && filteredRecommendations.map((place) => {
                                const status = statusCopy(place.compatibility?.petwayAssessment.status);
                                const added = course.some((item) => item.contentId === place.contentId);
                                return <article className="course-place-card" key={place.contentId}>
                                    <div className="course-place-image">{place.thumbnailUrl ? <img src={place.thumbnailUrl} alt="" loading="lazy" /> : <span>PetWay</span>}</div>
                                    <div className="course-place-copy">
                                        <div className="course-place-meta"><span>{TYPE_LABELS[place.contentTypeId] ?? "동반 장소"}</span><strong>{formatDistance(origin, place)}</strong></div>
                                        <h3>{place.title}</h3><p>{place.address ?? "주소 정보가 없습니다."}</p>
                                        <span className={`course-compatibility ${status.className}`}>{status.label}</span>
                                        {place.compatibility?.petwayAssessment.reasons[0] && <small>{place.compatibility.petwayAssessment.reasons[0]}</small>}
                                    </div>
                                    <button type="button" className="course-add-button" disabled={added || course.length >= maxStops} onClick={() => addPlace(place)} aria-label={`${place.title} 코스에 추가`}>
                                        <span aria-hidden="true">{added ? "✓" : "+"}</span>{added ? "담았어요" : "코스에 담기"}
                                    </button>
                                </article>;
                            })}
                        </div>
                    </section>

                    <aside className="course-builder">
                        <header><div><span>MY COURSE</span><h2>{selectedDay + 1}일차 코스</h2></div><button type="button" onClick={() => updateCourse([])} disabled={course.length === 0}>이 날짜 비우기</button></header>
                        <div className="course-day-tabs" aria-label="여행 날짜 선택">
                            {Array.from({ length: tripDays }, (_, index) => <button type="button" key={index} className={selectedDay === index ? "active" : ""} onClick={() => setSelectedDay(index)}><strong>DAY {index + 1}</strong><span>{courses[index]?.length ?? 0}곳</span></button>)}
                        </div>
                        {course.length > 0 && <div className="course-route-map"><KakaoMap center={{ latitude: course[0].latitude!, longitude: course[0].longitude! }} markers={markers} level={8} /></div>}
                        {course.length === 0 ? <div className="course-empty course-empty-builder"><strong>아직 정한 장소가 없어요</strong><span>추천 카드의 + 버튼으로 최대 {maxStops}곳을 담아보세요.</span></div> : <ol className="course-timeline">
                            {routeInfo.map(({ place, travel, stay }, index) => <li key={place.contentId}>
                                {travel !== null && <div className="course-travel-time">차량 이동 약 {travel}분</div>}
                                <div className="course-stop-number">{index + 1}</div><div className="course-stop-copy"><strong>{place.title}</strong><span>{TYPE_LABELS[place.contentTypeId] ?? "동반 장소"} · 약 {stay}분 머물기</span></div>
                                <div className="course-stop-actions"><button type="button" onClick={() => moveCourseItem(index, -1)} disabled={index === 0}>앞으로</button><button type="button" onClick={() => moveCourseItem(index, 1)} disabled={index === course.length - 1}>뒤로</button><button type="button" className="remove" onClick={() => updateCourse((current) => current.filter((item) => item.contentId !== place.contentId))}>삭제</button></div>
                            </li>)}
                        </ol>}
                        <footer className="course-summary"><div><span>{selectedDay + 1}일차 예상 일정</span><strong>{startTime} - {finishTime}</strong></div><div><span>전체 장소</span><strong>{courses.reduce((sum, day) => sum + day.length, 0)}곳</strong></div><small>날짜 탭을 선택해 일자별 코스를 편집하세요. 이동 시간은 좌표 간 거리로 계산한 참고값이에요.</small></footer>
                    </aside>
                </div>
            </div>
        </main>
    );
}
