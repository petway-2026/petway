/* 여행 조건에서 장소 후보를 모아 동반 가능 여부를 확인하고 AI 또는 대체 추천으로 날짜별 일정을 구성한다. */
import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { getPets } from "../../pet/api/petApi";
import type { Pet } from "../../pet/types/pet";
import {
    getPlaceCompatibility,
    searchAllPlaces,
    searchAllNearbyPlaces,
    type CompatibilityStatus,
    type PlaceCompatibility,
    type PlaceSummary,
} from "../../place/api/placeApi";
import KakaoMap from "../../place/components/KakaoMap";
import { loadKakaoMap } from "../../place/utils/loadKakaoMap";
import type { MapCenter, MapMarker } from "../../place/types/map";
import { getWeather } from "../../weather/api/weatherApi";
import type { WeatherForecastResponse, WeatherRegion } from "../../weather/types/weather";
import { createAiCourse, type AiCourseCandidate } from "../api/courseApi";
import { getDrivingRoute, getTransitRoute, type DrivingRoute, type TransitRoute } from "../api/routeApi";
import { saveTrip } from "../api/savedTripApi";
import FavoriteButton from "../../../shared/components/FavoriteButton";

import "../styles/travel-course.css";

const SEOUL_CENTER: MapCenter = { latitude: 37.5665, longitude: 126.978 };
const COMPATIBILITY_BATCH_SIZE = 6;

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

type DistrictOption = { name: string; center?: MapCenter };

// Keep the local selector independent from the tourism API. The API's
// sigungu codes are provider-specific, while these names are used to choose a
// useful search center for the course page.
const DISTRICTS: Record<string, DistrictOption[]> = {
    "1": [
        ["종로구", 37.5735, 126.9788], ["중구", 37.5636, 126.9976], ["용산구", 37.5326, 126.9907],
        ["성동구", 37.5633, 127.0368], ["광진구", 37.5385, 127.0823], ["동대문구", 37.5744, 127.0396],
        ["중랑구", 37.6063, 127.0927], ["성북구", 37.5894, 127.0167], ["강북구", 37.6396, 127.0257],
        ["도봉구", 37.6688, 127.0471], ["노원구", 37.6542, 127.0568], ["은평구", 37.6027, 126.9291],
        ["서대문구", 37.5791, 126.9368], ["마포구", 37.5663, 126.9014], ["양천구", 37.5169, 126.8666],
        ["강서구", 37.5509, 126.8495], ["구로구", 37.4954, 126.8874], ["금천구", 37.4569, 126.8955],
        ["영등포구", 37.5264, 126.8963], ["동작구", 37.5124, 126.9393], ["관악구", 37.4784, 126.9516],
        ["서초구", 37.4837, 127.0324], ["강남구", 37.5172, 127.0473], ["송파구", 37.5146, 127.1066],
        ["강동구", 37.5301, 127.1238],
    ].map(([name, latitude, longitude]) => ({ name: String(name), center: { latitude: Number(latitude), longitude: Number(longitude) } })),
    "2": ["중구", "동구", "미추홀구", "연수구", "남동구", "부평구", "계양구", "서구", "강화군", "옹진군"].map((name) => ({ name })),
    "3": ["동구", "중구", "서구", "유성구", "대덕구"].map((name) => ({ name })),
    "4": ["중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군", "군위군"].map((name) => ({ name })),
    "5": ["동구", "서구", "남구", "북구", "광산구"].map((name) => ({ name })),
    "6": ["중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구", "해운대구", "사하구", "금정구", "강서구", "연제구", "수영구", "사상구", "기장군"].map((name) => ({ name })),
    "7": ["중구", "남구", "동구", "북구", "울주군"].map((name) => ({ name })),
    "8": ["세종특별자치시"].map((name) => ({ name })),
    "31": ["수원시", "성남시", "의정부시", "안양시", "부천시", "광명시", "평택시", "안산시", "고양시", "과천시", "구리시", "남양주시", "오산시", "시흥시", "군포시", "의왕시", "하남시", "용인시", "파주시", "이천시", "안성시", "김포시", "화성시", "광주시", "양주시", "포천시", "여주시", "연천군", "가평군", "양평군"].map((name) => ({ name })),
    "32": ["춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시", "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "고성군", "양양군"].map((name) => ({ name })),
    "33": ["청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "증평군", "진천군", "괴산군", "음성군", "단양군"].map((name) => ({ name })),
    "34": ["천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시", "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군"].map((name) => ({ name })),
    "35": ["포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시", "문경시", "경산시", "군위군", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군", "봉화군", "울진군", "울릉군"].map((name) => ({ name })),
    "36": ["창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시", "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군"].map((name) => ({ name })),
    "37": ["전주시", "군산시", "익산시", "정읍시", "남원시", "김제시", "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"].map((name) => ({ name })),
    "38": ["목포시", "여수시", "순천시", "나주시", "광양시", "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군", "해남군", "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군"].map((name) => ({ name })),
    "39": ["제주시", "서귀포시"].map((name) => ({ name })),
};

const TYPE_LABELS: Record<string, string> = {
    "12": "관광지", "14": "문화시설", "15": "축제·행사", "28": "레포츠",
    "32": "숙박", "38": "쇼핑", "39": "음식점",
};

const STAY_MINUTES: Record<string, number> = {
    "12": 180, "14": 150, "15": 180, "28": 150, "32": 90, "38": 120, "39": 90,
};

const PROMPT_SUGGESTIONS = [
    "이동 시간을 짧게",
    "산책과 자연 위주",
    "카페와 맛집 중심",
    "비가 와도 좋은 실내 중심",
    "사람이 붐비지 않는 곳",
];

type TransportMode = "DRIVING" | "TRANSIT";
type RecommendationScope = "AVAILABLE" | "CONFIRMATION" | "UNAVAILABLE";
type SaveFeedback = {
    type: "success" | "error";
    message: string;
    tripId?: number;
};

const WEATHER_ICONS: Record<string, string> = {
    CLEAR: "☀️", PARTLY_CLOUDY: "🌤️", CLOUDY: "☁️", RAIN: "🌧️",
    RAIN_OR_SNOW: "🌨️", SNOW: "❄️", SHOWER: "🌦️", UNKNOWN: "🌡️",
};

interface Recommendation extends PlaceSummary {
    compatibility?: PlaceCompatibility;
}

function isCafePlace(place: Pick<Recommendation, "contentTypeId" | "title">): boolean {
    return place.contentTypeId === "39"
        && /(카페|커피|다락|다방|cafe|coffee|베이커리|디저트)/i.test(place.title);
}

function compactSpaceLabel(place: Recommendation): string {
    const indoor = place.indoor?.trim().toLowerCase();
    const outdoor = place.outdoor?.trim().toLowerCase();
    if (["y", "yes", "가능", "가능함"].includes(indoor ?? "")
        && ["y", "yes", "가능", "가능함"].includes(outdoor ?? "")) return "실내·야외";
    if (["y", "yes", "가능", "가능함"].includes(indoor ?? "")) return "실내";
    if (["y", "yes", "가능", "가능함"].includes(outdoor ?? "")) return "야외";
    return "공간 정보 확인 필요";
}

function formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function addDays(value: string, days: number): string {
    const date = new Date(`${value}T00:00:00`);
    date.setDate(date.getDate() + days);
    return formatDate(date);
}

function daysBetween(start: string, end: string): number {
    return Math.round((new Date(`${end}T00:00:00`).getTime() - new Date(`${start}T00:00:00`).getTime()) / 86400000) + 1;
}

function weatherRegionFor(code: string): WeatherRegion {
    if (["1", "2", "31"].includes(code)) return "SEOUL_INCHEON_GYEONGGI";
    if (code === "32") return "GANGWON_YEONGSEO";
    if (["3", "8", "34"].includes(code)) return "DAEJEON_SEJONG_CHUNGNAM";
    if (code === "33") return "CHUNGBUK";
    if (["5", "38"].includes(code)) return "GWANGJU_JEONNAM";
    if (code === "37") return "JEONBUK";
    if (["4", "35"].includes(code)) return "DAEGU_GYEONGBUK";
    if (["6", "7", "36"].includes(code)) return "BUSAN_ULSAN_GYEONGNAM";
    return "JEJU";
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
    const today = useMemo(() => formatDate(new Date()), []);
    const initialStartDate = useMemo(() => addDays(today, 7), [today]);
    const [pets, setPets] = useState<Pet[]>([]);
    const [regionCode, setRegionCode] = useState("1");
    const [districtName, setDistrictName] = useState("");
    const [tripDays, setTripDays] = useState(2);
    const [startDate, setStartDate] = useState(initialStartDate);
    const [endDate, setEndDate] = useState(addDays(initialStartDate, 1));
    const [selectedDay, setSelectedDay] = useState(0);
    const [origin, setOrigin] = useState<MapCenter>(SEOUL_CENTER);
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [regionPlaceCount, setRegionPlaceCount] = useState(0);
    const [courses, setCourses] = useState<Recommendation[][]>([[], []]);
    const [includeLodging, setIncludeLodging] = useState(false);
    const [selectedType, setSelectedType] = useState("all");
    const [recommendationScope, setRecommendationScope] = useState<RecommendationScope>("AVAILABLE");
    const [recommendationPage, setRecommendationPage] = useState(1);
    const [expandedCompatibilityId, setExpandedCompatibilityId] = useState<string | null>(null);
    const [startTime, setStartTime] = useState("10:00");
    const [customStayMinutes, setCustomStayMinutes] = useState<Record<string, number | "">>({});
    const [loading, setLoading] = useState(true);
    const [aiLoading, setAiLoading] = useState(false);
    const [aiSummary, setAiSummary] = useState<string | null>(null);
    const [travelPrompt, setTravelPrompt] = useState("");
    const [drivingRoute, setDrivingRoute] = useState<DrivingRoute | null>(null);
    const [transitRoute, setTransitRoute] = useState<TransitRoute | null>(null);
    const [transportMode, setTransportMode] = useState<TransportMode>("DRIVING");
    const [routeLoading, setRouteLoading] = useState(false);
    const [saveLoading, setSaveLoading] = useState(false);
    const [saveFeedback, setSaveFeedback] = useState<SaveFeedback | null>(null);
    const [weatherByDay, setWeatherByDay] = useState<Record<number, WeatherForecastResponse>>({});
    const [weatherLoading, setWeatherLoading] = useState(false);
    const [message, setMessage] = useState("서울 중심으로 추천 장소를 준비하고 있어요.");
    const saveInFlightRef = useRef(false);

    const primaryPet = pets.find((pet) => pet.isPrimary) ?? pets[0] ?? null;
    // 장소별 현실적인 체류시간을 우선하고, 하루가 과밀해지지 않도록
    // 수동 추가 장소는 최대 6곳까지만 담을 수 있게 한다.
    const maxStops = 6;
    const course = courses[selectedDay] ?? [];
    const selectedRegion = REGIONS.find((region) => region.code === regionCode) ?? REGIONS[0];
    const districtOptions = DISTRICTS[regionCode] ?? [];
    const dayDates = useMemo(() => Array.from({ length: tripDays }, (_, index) => addDays(startDate, index)), [startDate, tripDays]);
    const maximumTravelDate = useMemo(() => addDays(today, 90), [today]);
    const aiDisabledReason = !primaryPet
        ? "대표 반려견을 등록하면 AI 코스를 만들 수 있어요."
        : loading
            ? "추천 장소를 불러오고 있어요."
            : recommendations.length === 0
                ? "AI 코스를 만들 추천 장소가 없습니다."
                : undefined;

    const updateCourse = (updater: Recommendation[] | ((current: Recommendation[]) => Recommendation[])) => {
        setCourses((current) => {
            const next = Array.from({ length: tripDays }, (_, index) => current[index] ?? []);
            next[selectedDay] = typeof updater === "function" ? updater(next[selectedDay]) : updater;
            return next;
        });
    };

    const changeStayMinutes = (contentId: string, value: string) => {
        if (value.trim() === "") {
            setCustomStayMinutes((current) => ({ ...current, [contentId]: "" }));
            return;
        }
        const minutes = Number(value);
        if (!Number.isFinite(minutes)) return;
        setCustomStayMinutes((current) => ({
            ...current,
            [contentId]: Math.max(0, minutes),
        }));
    };

    useEffect(() => {
        getPets().then(setPets).catch(() => setPets([]));
    }, []);

    // 검색 결과를 거리순으로 정리하고 동반 판정을 묶음 조회한다. 판정 조회가 실패한 장소는 최종 후보에서 제외한다.
    const loadRecommendations = async (_areaCode: string, center: MapCenter, locationLabel: string, district?: string) => {
        setRecommendationPage(1);
        setLoading(true);
        setRecommendations([]);
        setRegionPlaceCount(0);
        setRecommendationScope("AVAILABLE");
        setExpandedCompatibilityId(null);
        setMessage(`${locationLabel} 주변에서 반려견 동반 장소를 찾고 있어요.`);
        try {
            const useAreaSearch = _areaCode !== "1" || Boolean(district);
            const response = useAreaSearch
                ? await searchAllPlaces(_areaCode)
                : await searchAllNearbyPlaces(center.latitude, center.longitude, 30000, (partial) => {
                const partialNearby = partial.items
                    .filter((place) => place.latitude !== null && place.longitude !== null)
                    .sort((left, right) => distanceKm(center, { latitude: left.latitude!, longitude: left.longitude! })
                        - distanceKm(center, { latitude: right.latitude!, longitude: right.longitude! }));
                setRegionPlaceCount(partialNearby.length);
                setRecommendations(partialNearby);
                setMessage(`${locationLabel} 주변 장소 ${partialNearby.length}곳을 먼저 보여드리고 있어요.`);
                }, {
                    fetchAll: true,
                });
            const scopedItems = response.items.filter((place) =>
                !district || (place.address ?? "").includes(district)
            );
            setRegionPlaceCount(scopedItems.length);
            const nearby = scopedItems
                .filter((place) => place.latitude !== null && place.longitude !== null)
                .sort((left, right) => {
                    const leftDistance = distanceKm(center, { latitude: left.latitude!, longitude: left.longitude! });
                    const rightDistance = distanceKm(center, { latitude: right.latitude!, longitude: right.longitude! });
                    return leftDistance - rightDistance;
                });

            const eligible: Recommendation[] = [];
            let attempted = 0;
            let providerFailureCount = 0;
            // 동시에 확인하는 개수는 제한하지만 현재 구현은 후보 목록 전체를 순차 묶음으로 확인한다.
            while (attempted < nearby.length) {
                const batchSize = Math.min(
                    COMPATIBILITY_BATCH_SIZE,
                    nearby.length - attempted,
                );
                const batch = nearby.slice(attempted, attempted + batchSize);
                const batchResult = await Promise.all(batch.map(async (place) => {
                    try {
                        return {
                            place: { ...place, compatibility: await getPlaceCompatibility(place.contentId) },
                            failed: false as const,
                        };
                    } catch {
                        return { failed: true as const };
                    }
                }));
                attempted += batch.length;
                batchResult.forEach((result) => {
                    if (result.failed) {
                        providerFailureCount += 1;
                    } else {
                        eligible.push(result.place);
                    }
                });
                setRecommendations([...eligible]);
                setMessage(`${locationLabel} 전체 ${nearby.length}곳 중 가까운 장소의 동반 조건을 확인하고 있어요.`);
            }
            setRecommendations(eligible);
            setMessage(`${locationLabel} 후보 ${scopedItems.length}곳 중 동반 조건을 확인한 ${eligible.length}곳을 추천했어요.${providerFailureCount > 0 ? ` 제공기관 오류로 ${providerFailureCount}곳은 추천에서 제외했어요.` : ""}`);
        } catch (error) {
            setRecommendations([]);
            setMessage(error instanceof Error ? error.message : "추천 장소를 불러오지 못했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { void loadRecommendations("1", SEOUL_CENTER, "서울특별시"); }, []);
    useEffect(() => {
        setCourses((current) => Array.from({ length: tripDays }, (_, index) => current[index] ?? []));
        setSelectedDay((current) => Math.min(current, tripDays - 1));
    }, [tripDays]);

    useEffect(() => {
        const controller = new AbortController();
        setWeatherLoading(true);
        setWeatherByDay({});
        Promise.all(dayDates.map(async (targetDate, index) => {
            try {
                const forecast = await getWeather({
                    latitude: origin.latitude,
                    longitude: origin.longitude,
                    targetDate,
                    region: weatherRegionFor(regionCode),
                }, controller.signal);
                return { index, forecast };
            } catch {
                return null;
            }
        })).then((results) => {
            if (controller.signal.aborted) return;
            setWeatherByDay(Object.fromEntries(results.filter((result): result is { index: number; forecast: WeatherForecastResponse } => result !== null).map((result) => [result.index, result.forecast])));
        }).finally(() => {
            if (!controller.signal.aborted) setWeatherLoading(false);
        });
        return () => controller.abort();
    }, [dayDates, regionCode, origin.latitude, origin.longitude]);

    const changeStartDate = (value: string) => {
        setStartDate(value);
        const nextEnd = endDate <= value ? addDays(value, 1) : endDate;
        const cappedEnd = nextEnd > addDays(value, 6) ? addDays(value, 6) : nextEnd;
        setEndDate(cappedEnd);
        setTripDays(daysBetween(value, cappedEnd));
    };

    const changeEndDate = (value: string) => {
        setEndDate(value);
        setTripDays(daysBetween(startDate, value));
    };

    const filteredRecommendations = useMemo(() => recommendations.filter((place) => {
        const status = place.compatibility?.petwayAssessment.status;
        const matchesScope = recommendationScope === "AVAILABLE"
            ? status === "AVAILABLE"
            : recommendationScope === "CONFIRMATION"
                ? status === "CONDITIONAL" || status === "NEED_CONFIRMATION"
                : status === "UNAVAILABLE";
        return (includeLodging || place.contentTypeId !== "32")
            && matchesScope
            && (selectedType === "all" || place.contentTypeId === selectedType);
    }), [includeLodging, recommendationScope, recommendations, selectedType]);
    const recommendationPageSize = 10;
    const recommendationTotalPages = Math.max(1, Math.ceil(filteredRecommendations.length / recommendationPageSize));
    const pagedRecommendations = useMemo(() => filteredRecommendations.slice(
        (recommendationPage - 1) * recommendationPageSize, recommendationPage * recommendationPageSize
    ), [filteredRecommendations, recommendationPage]);
    useEffect(() => { setRecommendationPage(1); }, [selectedType, includeLodging, recommendationScope]);

    const availableTypes = useMemo(() => Array.from(new Set(recommendations
        .filter((place) => includeLodging || place.contentTypeId !== "32")
        .map((place) => place.contentTypeId))), [includeLodging, recommendations]);

    useEffect(() => {
        const validStops = course.filter((place) => place.latitude !== null && place.longitude !== null);
        if (validStops.length < 2) {
            setDrivingRoute(null);
            setRouteLoading(false);
            return;
        }

        const controller = new AbortController();
        setRouteLoading(true);
        getDrivingRoute(validStops.map((place) => ({
            id: place.contentId,
            name: place.title,
            latitude: place.latitude!,
            longitude: place.longitude!,
        })), controller.signal)
            .then(setDrivingRoute)
            .catch((error: unknown) => {
                if (!(error instanceof DOMException && error.name === "AbortError")) setDrivingRoute(null);
            })
            .finally(() => {
                if (!controller.signal.aborted) setRouteLoading(false);
            });
        return () => controller.abort();
    }, [course]);

    useEffect(() => {
        const validStops = course.filter((place) => place.latitude !== null && place.longitude !== null);
        if (transportMode !== "TRANSIT" || validStops.length < 2) {
            setTransitRoute(null);
            return;
        }

        const controller = new AbortController();
        setRouteLoading(true);
        getTransitRoute(validStops.map((place) => ({
            id: place.contentId,
            name: place.title,
            latitude: place.latitude!,
            longitude: place.longitude!,
        })), controller.signal)
            .then(setTransitRoute)
            .catch((error: unknown) => {
                if (!(error instanceof DOMException && error.name === "AbortError")) setTransitRoute(null);
            })
            .finally(() => {
                if (!controller.signal.aborted) setRouteLoading(false);
            });
        return () => controller.abort();
    }, [course, transportMode]);

    // 실제 구간 응답이 있으면 이동시간에 우선 사용하고, 없으면 좌표 기반 추정값으로 일정 시간을 계산한다.
    const routeInfo = useMemo(() => course.map((place, index) => ({
        place,
        travel: index === 0 ? null
            : transportMode === "TRANSIT" && transitRoute?.legs[index - 1]
                ? transitRoute.legs[index - 1].durationMinutes
                : drivingRoute?.legs[index - 1]
                    ? Math.max(1, Math.ceil(drivingRoute.legs[index - 1].durationSeconds / 60))
                    : travelMinutes(course[index - 1], place),
        distance: index === 0 || place.latitude === null || place.longitude === null
            || course[index - 1].latitude === null || course[index - 1].longitude === null
            ? null
            : drivingRoute?.legs[index - 1]
                ? drivingRoute.legs[index - 1].distanceMeters / 1000
                : distanceKm(
                { latitude: course[index - 1].latitude!, longitude: course[index - 1].longitude! },
                { latitude: place.latitude, longitude: place.longitude },
            ),
        stay: customStayMinutes[place.contentId] === ""
            ? 0
            : Number(customStayMinutes[place.contentId] ?? STAY_MINUTES[place.contentTypeId] ?? 75),
        transitLeg: index === 0 ? null : transitRoute?.legs[index - 1] ?? null,
    })), [course, customStayMinutes, drivingRoute, transitRoute, transportMode]);

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
        label: String(index + 1),
    })), [course]);

    const routePath = useMemo<MapCenter[]>(() => transportMode === "DRIVING" && drivingRoute?.path.length
        ? drivingRoute.path
        : course.filter((place) => place.latitude !== null && place.longitude !== null)
            .map((place) => ({ latitude: place.latitude!, longitude: place.longitude! })), [course, drivingRoute, transportMode]);

    const addPlace = (place: Recommendation) => {
        if (course.some((item) => item.contentId === place.contentId)) return;
        if (place.compatibility?.petwayAssessment.status === "UNAVAILABLE") return;
        if (place.contentTypeId === "32") {
            if (!includeLodging || selectedDay === tripDays - 1 || course.some((item) => item.contentTypeId === "32")) return;
            updateCourse((current) => [...current, place]);
            return;
        }
        updateCourse((current) => {
            const lodgingIndex = current.findIndex((item) => item.contentTypeId === "32");
            if (lodgingIndex < 0) return [...current, place];
            return [...current.slice(0, lodgingIndex), place, ...current.slice(lodgingIndex)];
        });
    };

    // 사용자가 선택한 주소를 좌표로 바꿔 USER 출처의 장소로 추가한다. 공공데이터 동반 판정을 받은 장소는 아니다.
    const addCustomPlace = async () => {
        try {
            await loadKakaoMap();
        } catch (error) {
            window.alert(error instanceof Error ? error.message : "지도 주소 변환 기능을 불러오지 못했어요.");
            return;
        }
        const openPostcode = () => {
            new window.daum.Postcode({
                oncomplete: (data: { roadAddress?: string; jibunAddress?: string; buildingName?: string }) => {
                    const address = data.roadAddress || data.jibunAddress;
                    if (!address) return;
                    const title = window.prompt("일정에 표시할 장소명을 입력해 주세요.", data.buildingName || address)?.trim();
                    if (!title) return;
                    if (!window.kakao?.maps?.services?.Geocoder) {
                        window.alert("주소 좌표 변환 기능이 준비되지 않았어요. 페이지를 새로고침한 뒤 다시 시도해 주세요.");
                        return;
                    }
                    const geocoder = new window.kakao.maps.services.Geocoder();
                    geocoder.addressSearch(address, (result: Array<{ x: string; y: string }>, status: string) => {
                        if (status !== window.kakao.maps.services.Status.OK || result.length === 0) {
                            window.alert("주소의 지도 위치를 찾지 못했어요. 다른 주소로 다시 시도해 주세요.");
                            return;
                        }
                        const place: Recommendation = {
                            contentId: `custom-${Date.now()}`,
                            contentTypeId: "12",
                            areaCode: null,
                            sigunguCode: null,
                            title,
                            address,
                            latitude: Number(result[0].y),
                            longitude: Number(result[0].x),
                            thumbnailUrl: null,
                            source: "USER",
                        };
                        addPlace(place);
                    });
                },
            }).open();
        };

        if (window.daum?.Postcode) {
            openPostcode();
            return;
        }
        const scriptId = "daum-postcode-sdk";
        const existing = document.getElementById(scriptId);
        if (existing) {
            existing.addEventListener("load", openPostcode, { once: true });
            return;
        }
        const script = document.createElement("script");
        script.id = scriptId;
        script.src = "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
        script.async = true;
        script.onload = openPostcode;
        script.onerror = () => window.alert("주소 검색 서비스를 불러오지 못했어요.");
        document.head.appendChild(script);
    };

    const moveCourseItem = (index: number, direction: -1 | 1) => {
        const target = index + direction;
        if (target < 0 || target >= course.length) return;
        if (course[index].contentTypeId === "32" && direction === -1) return;
        if (course[target].contentTypeId === "32" && direction === 1) return;
        updateCourse((current) => {
            const next = [...current];
            [next[index], next[target]] = [next[target], next[index]];
            return next;
        });
    };

    // 현재 선택한 날짜뿐 아니라 모든 날짜의 장소·준비물 스냅샷을 저장한다. ref로 연속 클릭의 중복 요청을 막는다.
    const saveCurrentTrip = async () => {
        if (saveInFlightRef.current || !courses.some((day) => day.length > 0)) return;
        const suggestedTitle = `${selectedRegion.name} ${tripDays}일 여행`;
        const title = window.prompt("저장할 여행 제목을 입력해 주세요.", suggestedTitle)?.trim();
        if (!title) return;
        saveInFlightRef.current = true;
        setSaveLoading(true);
        setSaveFeedback(null);
        try {
            const savedTrip = await saveTrip({
                title,
                regionName: selectedRegion.name,
                startDate,
                endDate,
                transportMode,
                itinerary: {
                    days: dayDates.map((date, index) => ({
                        date,
                        places: (courses[index] ?? []).map((place) => ({
                            contentId: place.contentId,
                            contentTypeId: place.contentTypeId,
                            title: place.title,
                            address: place.address,
                            latitude: place.latitude,
                            longitude: place.longitude,
                            thumbnailUrl: place.thumbnailUrl,
                            preparations: place.compatibility?.petwayAssessment.requirements.map((item) => item.label) ?? [],
                        })),
                    })),
                },
            });
            setSaveFeedback({
                type: "success",
                message: "모든 날짜의 여행일정을 하나의 일정으로 저장했어요.",
                tripId: savedTrip.id,
            });
        } catch (error) {
            setSaveFeedback({
                type: "error",
                message: error instanceof Error ? error.message : "여행일정을 저장하지 못했습니다.",
            });
        } finally {
            saveInFlightRef.current = false;
            setSaveLoading(false);
        }
    };

    const changeRegion = (code: string) => {
        const region = REGIONS.find((item) => item.code === code) ?? REGIONS[0];
        setRegionCode(code);
        setDistrictName("");
        setOrigin(region.center);
        setSelectedType("all");
        setSelectedDay(0);
        setCourses(Array.from({ length: tripDays }, () => []));
        setAiSummary(null);
        void loadRecommendations(region.code, region.center, region.name);
    };

    const changeDistrict = (name: string) => {
        const district = districtOptions.find((item) => item.name === name);
        setDistrictName(name);
        setSelectedType("all");
        setSelectedDay(0);
        setCourses(Array.from({ length: tripDays }, () => []));
        setAiSummary(null);
        const center = district?.center ?? selectedRegion.center;
        setOrigin(center);
        void loadRecommendations(selectedRegion.code, center, `${selectedRegion.name} ${name}`, name);
    };

    const toggleLodging = (enabled: boolean) => {
        setIncludeLodging(enabled);
        setSelectedType("all");
        setAiSummary(null);
        if (!enabled) {
            setCourses((current) => current.map((day) => day.filter((place) => place.contentTypeId !== "32")));
        }
    };

    // AI 호출이 실패할 때 사용할 규칙 기반 대체 선택이다. 요청 키워드와 최근 선택 유형을 참고해 후보를 섞는다.
    const buildBalancedFallback = (places: Recommendation[], count: number) => {
        const prompt = travelPrompt.toLowerCase();
        const preferredTypes = prompt.includes("카페") || prompt.includes("맛집") || prompt.includes("음식")
            ? ["39", "12", "14"]
            : prompt.includes("실내") || prompt.includes("비") || prompt.includes("전시") || prompt.includes("문화")
                ? ["14", "38", "39"]
                : prompt.includes("산책") || prompt.includes("자연") || prompt.includes("야외")
                    ? ["12", "28", "39"]
                    : [];
        const selected: Recommendation[] = [];
        const remaining = [...places].sort((left, right) => {
            const leftPriority = preferredTypes.indexOf(left.contentTypeId);
            const rightPriority = preferredTypes.indexOf(right.contentTypeId);
            return (leftPriority < 0 ? 99 : leftPriority) - (rightPriority < 0 ? 99 : rightPriority);
        });
        while (selected.length < count && remaining.length > 0) {
            const usedTypes = new Set(selected.slice(-Math.max(1, maxStops - 1)).map((place) => place.contentTypeId));
            const index = remaining.findIndex((place) => !usedTypes.has(place.contentTypeId));
            selected.push(remaining.splice(index >= 0 ? index : 0, 1)[0]);
        }
        return selected;
    };

    // 선택한 후보를 날짜별로 나누고 하루 카페 수와 숙박 배치를 조정한다. 모델 출력이 곧 최종 일정은 아니다.
    const distributeCourse = (places: Recommendation[]) => {
        const activities = places.filter((place) => place.contentTypeId !== "32");
        const remainingActivities = [...activities];
        const lodgingCandidates = [
            ...places.filter((place) => place.contentTypeId === "32"),
            ...recommendations.filter((place) => place.contentTypeId === "32" && !places.some((selected) => selected.contentId === place.contentId)),
        ];
        setCourses(Array.from({ length: tripDays }, (_, dayIndex) => {
            const needsLodging = includeLodging && dayIndex < tripDays - 1;
            const activityLimit = maxStops - (needsLodging ? 1 : 0);
            const dayActivities: Recommendation[] = [];
            let cafeIncluded = false;
            while (dayActivities.length < activityLimit && remainingActivities.length > 0) {
                const nextIndex = remainingActivities.findIndex((place) => !isCafePlace(place) || !cafeIncluded);
                if (nextIndex < 0) break;
                const [nextPlace] = remainingActivities.splice(nextIndex, 1);
                dayActivities.push(nextPlace);
                cafeIncluded = cafeIncluded || isCafePlace(nextPlace);
            }
            const lodging = needsLodging ? lodgingCandidates[dayIndex] : undefined;
            return lodging ? [...dayActivities, lodging] : dayActivities;
        }));
        setSelectedDay(0);
    };

    const diversifyAiSelection = (places: Recommendation[], count: number) => {
        const groups = new Map<string, Recommendation[]>();
        places.forEach((place) => {
            const key = isCafePlace(place) ? "CAFE" : place.contentTypeId;
            const group = groups.get(key) ?? [];
            group.push(place);
            groups.set(key, group);
        });
        const selected: Recommendation[] = [];
        while (selected.length < count && groups.size > 0) {
            for (const [key, group] of groups) {
                const next = group.shift();
                if (next) selected.push(next);
                if (group.length === 0) groups.delete(key);
                if (selected.length >= count) break;
            }
        }
        return selected;
    };

    // 사용자 요청의 장소 유형과 출발지 거리를 점수화한 뒤 유형별로 번갈아 뽑아 AI 입력 크기를 제한한다.
    const selectAiCandidates = (places: Recommendation[], request: string, limit: number) => {
        const normalizedRequest = request.toLowerCase();
        const requestedTypes = new Set<string>();
        const keywordTypes: Array<[string[], string]> = [
            [["카페", "커피", "다방", "디저트", "베이커리"], "CAFE"],
            [["맛집", "식당", "음식", "점심", "저녁", "식사"], "39"],
            [["관광", "명소", "자연", "산책", "공원", "바다", "경치"], "12"],
            [["문화", "전시", "박물관", "미술관", "역사"], "14"],
            [["레포츠", "체험", "액티비티", "운동", "스포츠"], "28"],
            [["쇼핑", "시장", "몰", "백화점"], "38"],
            [["숙박", "호텔", "펜션", "리조트"], "32"],
        ];
        keywordTypes.forEach(([keywords, type]) => {
            if (keywords.some((keyword) => normalizedRequest.includes(keyword))) requestedTypes.add(type);
        });

        const scored = [...places].map((place) => {
            const key = isCafePlace(place) ? "CAFE" : place.contentTypeId;
            const distance = place.latitude !== null && place.longitude !== null
                ? distanceKm(origin, { latitude: place.latitude, longitude: place.longitude })
                : 999;
            const relevance = requestedTypes.has(key) ? 100 : requestedTypes.size === 0 ? 20 : 0;
            return { place, key, score: relevance - Math.min(distance, 50) };
        }).sort((left, right) => right.score - left.score);

        const groups = new Map<string, Recommendation[]>();
        scored.forEach(({ place, key }) => {
            const group = groups.get(key) ?? [];
            group.push(place);
            groups.set(key, group);
        });
        const selected: Recommendation[] = [];
        while (selected.length < limit && groups.size > 0) {
            for (const [key, group] of groups) {
                const next = group.shift();
                if (next) selected.push(next);
                if (group.length === 0) groups.delete(key);
                if (selected.length >= limit) break;
            }
        }
        return selected;
    };

    // 기존 코스 교체 확인 → 후보 압축 → AI 순위 요청 → 날짜별 배치 순서로 처리한다. 실패 시 대체 추천을 사용한다.
    const generateAiCourse = async () => {
        if (recommendations.length === 0 || !primaryPet) return;
        const hasCourse = courses.some((day) => day.length > 0);
        if (hasCourse && !window.confirm("현재 작성한 코스를 AI 추천 코스로 바꿀까요?")) return;

        setAiLoading(true);
        setAiSummary(null);
        const targetCount = tripDays * maxStops;
        // 실제 일정에 필요한 수를 기준으로 AI 후보만 동적으로 압축한다.
        // 장소 목록 화면의 전체 결과에는 영향을 주지 않는다.
        const aiCandidateBudget = Math.min(96, Math.max(40, targetCount * 8));
        const weatherSummary = dayDates.map((date, index) => {
            const forecast = weatherByDay[index];
            return forecast ? `${date} ${forecast.display.summary}` : `${date} 예보 확인 전`;
        }).join(" / ");
        // 현재 여행 화면은 AVAILABLE로 판정된 장소만 AI 입력 후보로 사용한다.
        const eligibleRecommendations = recommendations.filter((place) =>
            (includeLodging || place.contentTypeId !== "32")
            && place.compatibility?.petwayAssessment.status === "AVAILABLE"
        );
        // 장소 목록 화면과 AI 입력 모두 AVAILABLE 후보 전체를 사용한다.
        // 다만 이미 백엔드에서 동반 가능 여부를 판정했으므로, 모델에는
        // 이름·유형·거리처럼 코스 순서에 필요한 최소 정보만 전달한다.
        const candidates = selectAiCandidates(eligibleRecommendations, travelPrompt, aiCandidateBudget);
        try {
            const inputCandidates: AiCourseCandidate[] = candidates.map((place) => ({
                candidateId: place.contentId,
                name: place.title,
                category: `${isCafePlace(place) ? "카페" : TYPE_LABELS[place.contentTypeId] ?? "동반 장소"} · ${compactSpaceLabel(place)}`,
                compatibilityStatus: place.compatibility?.petwayAssessment.status === "AVAILABLE"
                    ? "AVAILABLE"
                    : place.compatibility?.petwayAssessment.status === "CONDITIONAL"
                        ? "CONDITIONAL"
                        : "NEED_CONFIRMATION",
                compatibilityReasons: [],
                requirements: place.compatibility?.petwayAssessment.requirements.map((item) => item.label) ?? [],
                distanceKm: place.latitude !== null && place.longitude !== null
                    ? Number(distanceKm(origin, { latitude: place.latitude, longitude: place.longitude }).toFixed(2))
                    : null,
                weatherSummary: null,
            }));
            const result = await createAiCourse({
                userIntent: `${selectedRegion.name}${districtName ? ` ${districtName}` : ""}, ${startDate}부터 ${endDate}까지 ${tripDays - 1}박 ${tripDays}일 반려견 동반 여행. 일정은 매일 10:00~20:00 안에서 자유롭게 구성한다. 첫 장소와 점심 전 장소는 선택한 시·군·구 및 현재 위치(origin)에서 가까운 곳을 우선하고, 같은 시간대에는 가까운 장소끼리 묶는다. 광진구에서 시작해 갑자기 먼 구의 식당으로 이동하는 식의 비효율적인 동선은 피한다. 하루에 먼 지역으로 이동해야 한다면 그 지역의 관광지·식당·카페를 함께 묶고, 장거리 이동은 하루 한 번 정도로 제한한다. 점심 식사는 12:00~14:00, 저녁 식사는 17:00~19:00에 배치하고, 카페는 식당으로 세지 않는다. 장소명에 카페·다락·다방·커피·베이커리·디저트가 들어간 음식점은 카페로 분류해 식사 시간대에서 제외한다. 카페는 하루 최대 1곳만 추천한다. 장소 유형별 기본 체류시간은 관광지 2~3시간, 문화시설 2~3시간, 레포츠 2~3시간, 쇼핑몰·아울렛 2시간 이상, 음식점 1~1.5시간, 카페 1~1.5시간, 숙박 1시간으로 잡고, 경복궁·대형 공원·아울렛처럼 넓은 장소는 짧게 배정하지 않는다. 이동시간과 체류시간을 합산해 실제로 10:00~20:00 안에 가능한 일정만 만든다. 최대 ${targetCount}개 장소를 날짜별·시간순 코스로 구성한다. 사용자 요청: ${travelPrompt.trim() || "현재 위치에서 가까운 장소부터 이동 동선이 자연스럽게 이어지는 여행"}. ${includeLodging ? "숙박 장소는 각 숙박일의 마지막 순서로 배치" : "숙박 장소는 추천에서 제외"}`,
                pet: { summary: `${primaryPet.petName}, ${primaryPet.weightKg}kg, ${primaryPet.sizeType ?? "체형 미확인"}, 활동성 ${primaryPet.activityLevel ?? "미확인"}` },
                weatherSummary,
                candidates: inputCandidates,
            });
            const byId = new Map(candidates.map((place) => [place.contentId, place]));
            const ranked = result.recommendations
                .sort((left, right) => left.rank - right.rank)
                .map((item) => byId.get(item.candidateId))
                .filter((place): place is Recommendation => place !== undefined)
                .slice(0, targetCount);
            const selected = diversifyAiSelection(ranked, targetCount);
            distributeCourse(selected);
            setAiSummary(`OpenAI 추천 적용됨 · ${result.summary}`);
        } catch {
            distributeCourse(buildBalancedFallback(candidates, targetCount));
            setAiSummary("규칙 기반 대체 추천 적용됨 · OpenAI 연결을 사용할 수 없어 동반 적합성, 거리, 장소 유형의 균형을 기준으로 코스 초안을 만들었어요.");
        } finally {
            setAiLoading(false);
        }
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
                    <div className="course-trip-badge"><strong>{selectedRegion.name}{districtName ? ` ${districtName}` : ""}</strong><span>{startDate} - {endDate} · {tripDays - 1}박 {tripDays}일</span></div>
                </header>

                <section className="course-settings" aria-label="여행 기본 설정">
                    <label className="course-select-field">여행 지역
                        <select value={regionCode} onChange={(event) => changeRegion(event.target.value)}>
                            {REGIONS.map((region) => <option key={region.code} value={region.code}>{region.name}</option>)}
                        </select>
                    </label>
                    <label className="course-select-field">시·군·구
                        <select value={districtName} onChange={(event) => changeDistrict(event.target.value)} disabled={districtOptions.length === 0}>
                            <option value="">전체 지역</option>
                            {districtOptions.map((district) => <option key={district.name} value={district.name}>{district.name}</option>)}
                        </select>
                    </label>
                    <label className="course-select-field">출발일
                        <input type="date" min={today} max={addDays(maximumTravelDate, -1)} value={startDate} onChange={(event) => changeStartDate(event.target.value)} />
                    </label>
                    <label className="course-select-field">도착일
                        <input type="date" min={addDays(startDate, 1)} max={addDays(startDate, 6) < maximumTravelDate ? addDays(startDate, 6) : maximumTravelDate} value={endDate} onChange={(event) => changeEndDate(event.target.value)} />
                    </label>
                    <div className="course-pet-summary">
                        <span>함께 갈 반려견</span>
                        {primaryPet ? <strong>{primaryPet.petName}</strong> : <Link to="/pets/new">반려견 등록하기</Link>}
                        <small>{primaryPet ? `${primaryPet.weightKg}kg · ${primaryPet.sizeType ?? "체형 확인 필요"}` : "등록하면 장소별 동반 가능 여부를 확인해 드려요."}</small>
                    </div>
                    <label className="course-time-field">출발 시간<input type="time" min="10:00" max="20:00" value={startTime} onChange={(event) => setStartTime(event.target.value)} /></label>
                </section>

                <p className="course-status" role="status">{message}</p>

                <section className="course-ai-panel">
                    <div className="course-ai-heading"><span>AI COURSE</span><strong>어떤 여행을 원하는지 알려주세요</strong><p>{aiSummary ?? "취향을 자세히 적을수록 더 자연스러운 날짜별 코스를 만들 수 있어요."}</p></div>
                    <div className="course-prompt-area">
                        <label htmlFor="travel-course-prompt">여행 요청</label>
                        <textarea id="travel-course-prompt" value={travelPrompt} onChange={(event) => setTravelPrompt(event.target.value)} maxLength={500} rows={3} placeholder="예: 첫날은 강아지와 오래 산책하고, 둘째 날은 이동을 줄여 카페와 실내 관광지 위주로 짜줘. 점심은 음식점을 꼭 넣어줘." />
                        <div className="course-prompt-footer"><div className="course-prompt-suggestions">{PROMPT_SUGGESTIONS.map((suggestion) => <button type="button" key={suggestion} onClick={() => setTravelPrompt((current) => current.includes(suggestion) ? current : `${current}${current ? ", " : ""}${suggestion}`)}>{suggestion}</button>)}</div><small>{travelPrompt.length} / 500</small></div>
                    </div>
                    <div className="course-ai-actions">
                        <label className="course-lodging-option"><input type="checkbox" checked={includeLodging} onChange={(event) => toggleLodging(event.target.checked)} /><span><strong>숙박도 추천받기</strong><small>숙박일의 마지막 장소로만 추가돼요.</small></span></label>
                        <button
                            type="button"
                            className="course-ai-generate"
                            onClick={() => void generateAiCourse()}
                            disabled={aiLoading || aiDisabledReason !== undefined}
                            aria-busy={aiLoading}
                            title={aiDisabledReason}
                        >
                            {aiLoading ? "코스 만드는 중…" : courses.some((day) => day.length > 0) ? "AI로 다시 짜기" : "AI가 코스 짜기"}
                        </button>
                    </div>
                </section>

                <section className="course-trip-save" aria-label="전체 여행일정 저장">
                    <div>
                        <span>MY TRIP</span>
                        <strong>{tripDays}일 전체 여행일정을 저장</strong>
                        <small>DAY 1부터 마지막 날까지 담은 장소와 이동 정보를 한 번에 저장해요.</small>
                    </div>
                    <button type="button" onClick={() => void saveCurrentTrip()} disabled={saveLoading || !courses.some((day) => day.length > 0)}>
                        {saveLoading ? "저장 중…" : "전체 일정 저장"}
                    </button>
                    {saveFeedback && (
                        <div
                            className={`course-save-feedback ${saveFeedback.type}`}
                            role={saveFeedback.type === "error" ? "alert" : "status"}
                        >
                            <span>{saveFeedback.message}</span>
                            {saveFeedback.tripId !== undefined && (
                                <Link to={`/my-trips/${saveFeedback.tripId}`}>저장 일정 확인</Link>
                            )}
                        </div>
                    )}
                </section>

                <div className="course-workspace">
                    <section className="course-recommendations">
                        <header><div><span>RECOMMENDATIONS</span><h2>{primaryPet ? `${primaryPet.petName}와 갈 만한 곳` : "지역 추천 장소"}</h2></div><strong>{recommendationScope === "AVAILABLE" ? "바로 동반 가능" : recommendationScope === "CONFIRMATION" ? "방문 전 확인" : "동반 불가"} {filteredRecommendations.length} / 전체 {regionPlaceCount}곳</strong></header>
                        <div className="course-filter-groups">
                        <div className="course-scope-filters" aria-label="동반 가능 여부">
                            <button type="button" className={`course-scope-button available ${recommendationScope === "AVAILABLE" ? "active" : ""}`} onClick={() => setRecommendationScope("AVAILABLE")}>바로 동반 가능</button>
                            <button type="button" className={`course-scope-button confirmation ${recommendationScope === "CONFIRMATION" ? "active" : ""}`} onClick={() => setRecommendationScope("CONFIRMATION")}>방문 전 확인</button>
                            <button type="button" className={`course-scope-button unavailable ${recommendationScope === "UNAVAILABLE" ? "active" : ""}`} onClick={() => setRecommendationScope("UNAVAILABLE")}>동반 불가</button>
                        </div>
                        <div className="course-type-filters">
                            <button type="button" className={selectedType === "all" ? "active" : ""} onClick={() => setSelectedType("all")}>전체</button>
                            {availableTypes.map((type) => <button type="button" key={type} className={selectedType === type ? "active" : ""} onClick={() => setSelectedType(type)}>{TYPE_LABELS[type] ?? "기타"}</button>)}
                        </div>
                        </div>
                        <div className="course-card-list">
                            {loading && <div className="course-empty paw-loading"><span aria-hidden="true">🐾</span><strong>가까운 동반 장소를 찾고 있어요.</strong></div>}
                            {!loading && pagedRecommendations.map((place) => {
                                const status = statusCopy(place.compatibility?.petwayAssessment.status);
                                const assessment = place.compatibility?.petwayAssessment;
                                const expanded = expandedCompatibilityId === place.contentId;
                                const added = course.some((item) => item.contentId === place.contentId);
                                const isUnavailable = place.compatibility?.petwayAssessment.status === "UNAVAILABLE";
                                const lodgingUnavailable = place.contentTypeId === "32"
                                    && (!includeLodging || selectedDay === tripDays - 1 || course.some((item) => item.contentTypeId === "32"));
                                return <article className="course-place-card" key={place.contentId}>
                                    <div className="course-place-image">{place.thumbnailUrl ? <img src={place.thumbnailUrl} alt="" loading="lazy" /> : <span>PetWay</span>}</div>
                                    <div className="course-place-copy">
                                        <div className="course-place-meta"><span>{TYPE_LABELS[place.contentTypeId] ?? "동반 장소"}</span><strong>{formatDistance(origin, place)}</strong></div>
                                        <div className="course-place-title"><h3>{place.title}</h3><FavoriteButton targetType="PLACE" targetId={place.contentId} title={place.title} address={place.address} thumbnailUrl={place.thumbnailUrl} /></div><p>{place.address ?? "주소 정보가 없습니다."}</p>
                                        {place.operatingHours && <small>운영시간 · {place.operatingHours}</small>}
                                        {place.closedDays && <small>휴무일 · {place.closedDays}</small>}
                                        <button type="button" className={`course-compatibility ${status.className}`} aria-expanded={expanded} onClick={() => setExpandedCompatibilityId((current) => current === place.contentId ? null : place.contentId)}>
                                            {status.label}<span aria-hidden="true">{expanded ? "⌃" : "⌄"}</span>
                                        </button>
                                        {!expanded && assessment?.reasons[0] && <small>{assessment.reasons[0]}</small>}
                                        {expanded && assessment && <div className="course-compatibility-details">
                                            {assessment.reasons.length > 0 && <><strong>{assessment.status === "AVAILABLE" ? "이용 안내" : "방문 전 확인할 내용"}</strong><ul>{assessment.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul></>}
                                            {assessment.requirements.length > 0 && <><strong>방문 준비사항</strong><ul>{assessment.requirements.map((requirement) => <li key={`${requirement.type}-${requirement.scope}`}>{requirement.scope === "INDOOR" ? "실내 이용 시 " : requirement.scope === "OUTDOOR" ? "실외 이용 시 " : ""}{requirement.label} 준비가 필요합니다.</li>)}</ul></>}
                                            {place.compatibility?.officialInformation.additionalInformation && <small>공식 안내 · {place.compatibility.officialInformation.additionalInformation}</small>}
                                        </div>}
                                        {place.contentTypeId === "32" && <small className="course-lodging-notice">숙박일의 마지막 코스로 추가되며 예약은 해당 업체에서 확인해 주세요.</small>}
                                    </div>
                                    <button type="button" className="course-add-button" disabled={added || isUnavailable || lodgingUnavailable} onClick={() => addPlace(place)} aria-label={`${place.title} 코스에 추가`}>
                                        <span aria-hidden="true">{added ? "✓" : "+"}</span>{added ? "담았어요" : isUnavailable || lodgingUnavailable ? "추가 불가" : "코스에 담기"}
                                    </button>
                                </article>;
                            })}
                        </div>
                        {!loading && recommendationTotalPages > 1 && <nav className="facility-pagination" aria-label="추천 장소 페이지">
                            <button type="button" onClick={() => setRecommendationPage((value) => Math.max(1, value - 1))} disabled={recommendationPage === 1}>이전</button>
                            <span>{recommendationPage} / {recommendationTotalPages}</span>
                            <button type="button" onClick={() => setRecommendationPage((value) => Math.min(recommendationTotalPages, value + 1))} disabled={recommendationPage === recommendationTotalPages}>다음</button>
                        </nav>}
                    </section>

                    <aside className="course-builder">
                        <header><div><span>MY COURSE</span><h2>{selectedDay + 1}일차 코스</h2></div><div className="course-builder-actions"><button type="button" className="course-add-custom" onClick={addCustomPlace}>+ 장소 직접 추가</button><button type="button" onClick={() => updateCourse([])} disabled={course.length === 0}>이 날짜 비우기</button></div></header>
                        <div className="course-day-tabs" aria-label="여행 날짜 선택">
                            {dayDates.map((date, index) => <button type="button" key={date} className={selectedDay === index ? "active" : ""} onClick={() => setSelectedDay(index)}><strong>DAY {index + 1}</strong><span>{date.slice(5).replace("-", ".")} · {weatherByDay[index] ? WEATHER_ICONS[weatherByDay[index].weatherCondition ?? "UNKNOWN"] : "🌡️"}</span><small>{courses[index]?.length ?? 0}곳</small></button>)}
                        </div>
                        <div className="course-day-weather">
                            {weatherLoading ? <span>기상청 예보를 확인하고 있어요.</span> : weatherByDay[selectedDay] ? <><span className="course-day-weather-icon">{WEATHER_ICONS[weatherByDay[selectedDay].weatherCondition ?? "UNKNOWN"]}</span><div><strong>{dayDates[selectedDay]} · {weatherByDay[selectedDay].display.weather}</strong><p>{weatherByDay[selectedDay].display.temperature} · 강수 {weatherByDay[selectedDay].display.precipitation}</p><small>{weatherByDay[selectedDay].forecastType === "SEASONAL_OUTLOOK" ? "장기 일정은 일별 예보가 아닌 월별 기후 전망이에요." : weatherByDay[selectedDay].display.summary}</small></div></> : <span>이 날짜에 제공된 기상청 예보가 아직 없어요.</span>}
                        </div>
                        <div className="course-transport-tabs" aria-label="이동수단 선택"><button type="button" className={transportMode === "DRIVING" ? "active" : ""} onClick={() => setTransportMode("DRIVING")}>🚗 자동차</button><button type="button" className={transportMode === "TRANSIT" ? "active" : ""} onClick={() => setTransportMode("TRANSIT")}>🚌 대중교통</button></div>
                        {course.length > 0 && <div className="course-route-map"><KakaoMap center={{ latitude: course[0].latitude!, longitude: course[0].longitude! }} markers={markers} routePath={routePath} level={8} /><small className="course-route-source">{routeLoading ? "경로를 찾고 있어요…" : transportMode === "TRANSIT" && transitRoute ? "ODsay 추천 대중교통 경로" : drivingRoute ? "카카오모빌리티 실제 자동차 경로" : "직선거리 기반 예상 경로"}</small></div>}
                        {course.length === 0 ? <div className="course-empty course-empty-builder"><strong>아직 정한 장소가 없어요</strong><span>추천 카드나 ‘장소 직접 추가’로 원하는 장소를 담아보세요.</span></div> : <ol className="course-timeline">
                            {routeInfo.map(({ place, travel, distance, stay, transitLeg }, index) => <li key={place.contentId}>
                                {travel !== null && <div className="course-travel-time">{index} → {index + 1} 이동 · {transportMode === "TRANSIT" && transitLeg ? `대중교통 약 ${travel}분 · 환승 ${transitLeg.transfers}회 · ${transitLeg.fare.toLocaleString()}원` : `차량 약 ${travel}분${distance !== null ? ` · 약 ${distance.toFixed(1)}km` : ""}`}{transportMode === "TRANSIT" && transitLeg && <span className="course-transit-steps">{transitLeg.steps.map((step) => `${step.type === "WALK" ? "도보" : step.type === "BUS" ? "버스" : "지하철"} ${step.durationMinutes}분${step.description ? ` (${step.description})` : ""}`).join(" → ")}</span>}</div>}
                                <div className="course-stop-number">{index + 1}</div><div className="course-stop-copy"><strong>{place.title}</strong><span>{TYPE_LABELS[place.contentTypeId] ?? "동반 장소"} · <label className="course-stay-editor">머무르기 <input type="number" min="0" step="1" value={stay === 0 ? "" : stay} onChange={(event) => changeStayMinutes(place.contentId, event.target.value)} aria-label={`${place.title} 머무는 시간(분)`} />분</label>{stay === 0 && <em className="course-stay-help">시간을 작성해주세요</em>}</span></div>
                                <div className="course-stop-actions"><button type="button" onClick={() => moveCourseItem(index, -1)} disabled={index === 0 || place.contentTypeId === "32"}>앞으로</button><button type="button" onClick={() => moveCourseItem(index, 1)} disabled={index === course.length - 1 || course[index + 1]?.contentTypeId === "32"}>뒤로</button><button type="button" className="remove" onClick={() => updateCourse((current) => current.filter((item) => item.contentId !== place.contentId))}>삭제</button></div>
                            </li>)}
                        </ol>}
                        <footer className="course-summary"><div><span>{selectedDay + 1}일차 예상 일정</span><strong>{startTime} - {finishTime}</strong></div><div><span>전체 장소</span><strong>{courses.reduce((sum, day) => sum + day.length, 0)}곳</strong></div><small>{transportMode === "TRANSIT" && transitRoute ? `ODsay 추천 대중교통 기준 · 총 교통비 약 ${transitRoute.totalFare.toLocaleString()}원` : drivingRoute ? "지도와 구간별 시간·거리는 카카오모빌리티 추천 자동차 경로 기준입니다." : "길찾기 정보를 불러오지 못하면 좌표 간 직선거리 기반 참고값을 사용합니다."}</small></footer>
                    </aside>
                </div>
            </div>
        </main>
    );
}
