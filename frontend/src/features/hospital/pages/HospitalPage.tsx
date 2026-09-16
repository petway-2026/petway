/* 병원·약국의 지역 검색, 지도 범위, 목록 페이지와 선택 시설을 함께 관리한다. */
import {
    type FormEvent,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";

import KakaoMap from "../../place/components/KakaoMap";
import FavoriteButton from "../../../shared/components/FavoriteButton";
import ReviewButton from "../../../shared/components/ReviewButton";
import RadiusPicker, { type RadiusOption } from "../../../shared/components/RadiusPicker";
import type { MapBounds, MapCenter, MapMarker } from "../../place/types/map";
import { getHospitals } from "../api/hospitalApi";
import type { Hospital } from "../types/hospital";

import "../styles/hospital.css";

const KOREA_CENTER: MapCenter = {
    latitude: 36.35,
    longitude: 127.85,
};
const DEFAULT_MAP_CENTER: MapCenter = {
    latitude: 37.5665,
    longitude: 126.978,
};
// The backend/API accepts up to 100 records per page. Keeping the page size
// at the maximum gives the map enough markers while the list remains paged.
const HOSPITAL_PAGE_SIZE = 100;
const MAP_QUERY_SIZE = 100;

const REGIONS = [
    { label: "전국", value: "", center: KOREA_CENTER, level: 13 },
    { label: "서울", value: "서울특별시", center: { latitude: 37.5665, longitude: 126.978 }, level: 9 },
    { label: "경기", value: "경기도", center: { latitude: 37.4138, longitude: 127.5183 }, level: 11 },
    { label: "인천", value: "인천광역시", center: { latitude: 37.4563, longitude: 126.7052 }, level: 9 },
    { label: "부산", value: "부산광역시", center: { latitude: 35.1796, longitude: 129.0756 }, level: 9 },
] as const;
const DISTRICTS: Record<string, string[]> = {
    서울특별시: ["전체", "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"],
    경기도: ["전체", "수원시", "성남시", "고양시", "용인시", "부천시", "안산시", "안양시", "남양주시", "화성시", "평택시", "의정부시", "시흥시", "파주시", "김포시", "광주시", "광명시", "군포시", "하남시", "오산시", "이천시", "안성시", "구리시", "의왕시", "양주시", "포천시", "여주시"],
    인천광역시: ["전체", "중구", "동구", "미추홀구", "연수구", "남동구", "부평구", "계양구", "서구", "강화군", "옹진군"],
    부산광역시: ["전체", "중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구", "해운대구", "사하구", "금정구", "강서구", "연제구", "수영구", "사상구", "기장군"],
    광주광역시: ["전체", "동구", "서구", "남구", "북구", "광산구"],
    전라북도: ["전체", "전주시", "군산시", "익산시", "정읍시", "남원시", "김제시", "완주군"],
    전라남도: ["전체", "목포시", "여수시", "순천시", "나주시", "광양시", "담양군", "무안군", "해남군", "영암군"],
    대전광역시: ["전체", "동구", "중구", "서구", "유성구", "대덕구"],
    대구광역시: ["전체", "중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군", "군위군"],
    울산광역시: ["전체", "중구", "남구", "동구", "북구", "울주군"],
    세종특별자치시: ["전체"],
    강원특별자치도: ["전체", "춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시", "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "고성군", "양양군"],
    충청북도: ["전체", "청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "진천군", "괴산군", "음성군", "단양군", "증평군"],
    충청남도: ["전체", "천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시", "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군"],
    경상북도: ["전체", "포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시", "문경시", "경산시", "군위군", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군", "봉화군", "울진군", "울릉군"],
    경상남도: ["전체", "창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시", "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군"],
    제주특별자치도: ["전체", "제주시", "서귀포시"]
};

export default function HospitalPage() {
    const [hospitals, setHospitals] = useState<Hospital[]>([]);
    const [mapHospitals, setMapHospitals] = useState<Hospital[]>([]);
    const [facilityType, setFacilityType] = useState<"HOSPITAL" | "PHARMACY">("HOSPITAL");
    const [page, setPage] = useState(1);
    const [totalCount, setTotalCount] = useState(0);
    const [region, setRegion] = useState("");
    const [addressQuery, setAddressQuery] = useState("");
    const [province, setProvince] = useState("");
    const [district, setDistrict] = useState("");
    const [nameInput, setNameInput] = useState("");
    const [hospitalName, setHospitalName] = useState("");
    const [center, setCenter] = useState<MapCenter>(DEFAULT_MAP_CENTER);
    // Kakao level 1 is the closest practical view (roughly a 100m area).
    const [mapLevel, setMapLevel] = useState(1);
    const [loading, setLoading] = useState(true);
    const [locationLoading, setLocationLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [locationMessage, setLocationMessage] = useState<string | null>(null);
    const [currentLocation, setCurrentLocation] = useState<MapCenter | null>(null);
    const [pendingLocation, setPendingLocation] = useState<MapCenter | null>(null);
    const [radiusPickerOpen, setRadiusPickerOpen] = useState(false);
    const [searchRadiusMeters, setSearchRadiusMeters] = useState<number | null>(null);
    const [draftBounds, setDraftBounds] = useState<MapBounds | null>(null);
    const [appliedBounds, setAppliedBounds] = useState<MapBounds | null>(null);
    const [mapMoved, setMapMoved] = useState(false);
    const [mapAreaSearch, setMapAreaSearch] = useState(false);
    const [selectedHospitalId, setSelectedHospitalId] = useState<string | null>(null);
    const applyNextBoundsRef = useRef(true);

    useEffect(() => {
        let active = true;

        const loadHospitals = async () => {
            setLoading(true);
            setError(null);
            // Prevent the previous tab's facilities from remaining visible
            // while the new hospital/pharmacy request is in flight.
            setHospitals([]);
            setMapHospitals([]);

            try {
                const response = await getHospitals(
                    addressQuery || region || undefined,
                    hospitalName || undefined,
                    facilityType,
                    page,
                    HOSPITAL_PAGE_SIZE
                );

                if (!active) {
                    return;
                }

                setHospitals(response.hospitals);
                setTotalCount(response.totalCount);
                if (addressQuery) {
                    const firstLocated = response.hospitals.find((hospital) => hospital.latitude !== null && hospital.longitude !== null);
                    if (firstLocated) {
                        setCenter({ latitude: firstLocated.latitude as number, longitude: firstLocated.longitude as number });
                        setMapLevel(district ? 7 : 9);
                        applyNextBoundsRef.current = true;
                    }
                }

                // The list is paged, but the map should contain every
                // matching facility. Load the remaining API pages for map
                // markers after rendering the current list page.
                const mapTotalPages = Math.max(1, Math.ceil(response.totalCount / MAP_QUERY_SIZE));
                setMapHospitals([]);
                try {
                    for (let mapPage = 1; mapPage <= mapTotalPages; mapPage += 1) {
                        const mapResponse = await getHospitals(
                            addressQuery || region || undefined,
                            hospitalName || undefined,
                            facilityType,
                            mapPage,
                            MAP_QUERY_SIZE
                        );
                        if (!active) return;
                        setMapHospitals((previous) => {
                            const merged = [...previous, ...mapResponse.hospitals];
                            return Array.from(new Map(merged.map((hospital) => [hospital.id, hospital])).values());
                        });
                    }
                } catch (mapError) {
                    console.warn("지도용 추가 시설 조회 실패; 현재까지 받은 마커를 유지합니다.", mapError);
                }
            } catch (requestError) {
                if (!active) {
                    return;
                }

                setHospitals([]);
                setMapHospitals([]);
                setTotalCount(0);
                setError(
                    requestError instanceof Error
                        ? requestError.message
                        : "동물병원 정보를 불러오지 못했습니다."
                );
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        };

        loadHospitals();

        return () => {
            active = false;
        };
    }, [region, addressQuery, hospitalName, facilityType, page, district]);

    const totalPages = Math.max(1, Math.ceil(totalCount / HOSPITAL_PAGE_SIZE));

    const mapDisplayedHospitals = useMemo(
        () => mapHospitals.filter((hospital) =>
            hospital.latitude !== null && hospital.longitude !== null &&
            (searchRadiusMeters === null || currentLocation === null || (() => {
                const radians = (degree: number) => degree * Math.PI / 180;
                const latitudeDelta = radians(hospital.latitude! - currentLocation.latitude);
                const longitudeDelta = radians(hospital.longitude! - currentLocation.longitude);
                const a = Math.sin(latitudeDelta / 2) ** 2 + Math.cos(radians(currentLocation.latitude)) * Math.cos(radians(hospital.latitude!)) * Math.sin(longitudeDelta / 2) ** 2;
                return 6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) <= searchRadiusMeters;
            })()) &&
            (appliedBounds === null || (
                hospital.latitude >= appliedBounds.south &&
                hospital.latitude <= appliedBounds.north &&
                hospital.longitude >= appliedBounds.west &&
                hospital.longitude <= appliedBounds.east
            ))),
        [appliedBounds, currentLocation, mapHospitals, searchRadiusMeters]
    );

    const orderedHospitals = useMemo(() => {
        const source = (mapAreaSearch ? mapDisplayedHospitals : hospitals)
            .filter((hospital) => hospital.latitude !== null && hospital.longitude !== null)
            .sort((left, right) => {
                if (!currentLocation) return 0;
                const distance = (hospital: Hospital) => (hospital.latitude! - currentLocation.latitude) ** 2 + (hospital.longitude! - currentLocation.longitude) ** 2;
                return distance(left) - distance(right);
            });
        if (selectedHospitalId === null) return source;
        const selected = source.find((hospital) => hospital.id === selectedHospitalId);
        return selected
            ? [selected, ...source.filter((hospital) => hospital.id !== selectedHospitalId)]
            : source;
    }, [currentLocation, hospitals, mapAreaSearch, mapDisplayedHospitals, selectedHospitalId]);

    const markers = useMemo<MapMarker[]>(
        () => [
            ...orderedHospitals
            .map((hospital) => ({
                id: hospital.id,
                title: hospital.name,
                latitude: hospital.latitude as number,
                longitude: hospital.longitude as number,
                kind: facilityType === "PHARMACY" ? "pharmacy" as const : "hospital" as const,
            })),
            ...(currentLocation ? [{
                id: "current-location",
                title: "현재 위치",
                latitude: currentLocation.latitude,
                longitude: currentLocation.longitude,
                kind: "current-location" as const,
            }] : []),
        ],
        [currentLocation, facilityType, orderedHospitals]
    );

    const handleBoundsChange = useCallback((bounds: MapBounds) => {
        setDraftBounds(bounds);
        if (applyNextBoundsRef.current) {
            if (searchRadiusMeters === null) setAppliedBounds(bounds);
            setMapMoved(false);
            if (locationLoading) {
                setMapAreaSearch(true);
                setLocationLoading(false);
            }
            applyNextBoundsRef.current = false;
            return;
        }
        setMapMoved(true);
    }, [locationLoading, searchRadiusMeters]);

    const selectRegion = (selectedRegion: typeof REGIONS[number]) => {
        applyNextBoundsRef.current = true;
        setRegion(selectedRegion.value);
        setCenter(selectedRegion.center);
        setMapLevel(selectedRegion.level);
        setLocationMessage(null);
        setPage(1);
    };

    const searchByName = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setMapAreaSearch(false);
        setSearchRadiusMeters(null);
        setCurrentLocation(null);
        setAppliedBounds(null);
        setDraftBounds(null);
        setMapMoved(false);
        applyNextBoundsRef.current = true;
        setSelectedHospitalId(null);
        setAddressQuery([province, district].filter(Boolean).join(" "));
        setHospitalName(nameInput.trim());
        setPage(1);
    };

    const clearSearchFilters = () => {
        setProvince("");
        setDistrict("");
        setAddressQuery("");
        setNameInput("");
        setHospitalName("");
        setPage(1);
        setSelectedHospitalId(null);
    };

    const moveToHospital = (hospital: Hospital) => {
        if (hospital.latitude === null || hospital.longitude === null) {
            return;
        }

        setSelectedHospitalId(hospital.id);
        applyNextBoundsRef.current = true;
        setCenter({
            latitude: hospital.latitude,
            longitude: hospital.longitude,
        });
        setMapLevel(4);
    };

    const handleHospitalMarkerSelect = useCallback((marker: MapMarker | null) => {
        if (!marker || (marker.kind !== "hospital" && marker.kind !== "pharmacy")) {
            setSelectedHospitalId(null);
            return;
        }

        const hospital = mapHospitals.find((candidate) => candidate.id === String(marker.id));
        if (!hospital) return;

        setSelectedHospitalId(hospital.id);
        window.requestAnimationFrame(() => {
            document.getElementById(`hospital-${hospital.id}`)?.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
        });
    }, [mapHospitals]);

    const moveToCurrentLocation = () => {
        clearSearchFilters();
        if (!navigator.geolocation) {
            setLocationMessage("이 브라우저에서는 현재 위치를 사용할 수 없습니다.");
            return;
        }

        setLocationMessage("현재 위치를 확인하고 있습니다.");
        setLocationLoading(true);

        navigator.geolocation.getCurrentPosition(
            (position) => {
                // GPS stays in the browser. It is used only as the map center
                // and is never included in the hospital API request.
                const location = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                };
                setLocationLoading(false);
                setPendingLocation(location);
                setRadiusPickerOpen(true);
                setLocationMessage("검색할 반경을 선택해주세요.");
            },
            () => {
                setLocationLoading(false);
                setLocationMessage("현재 위치를 확인할 수 없습니다.");
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 60000,
            }
        );
    };

    const selectCurrentLocationRadius = (option: RadiusOption) => {
        if (!pendingLocation) return;
        applyNextBoundsRef.current = true;
        setCurrentLocation(pendingLocation);
        setCenter(pendingLocation);
        setMapLevel(option.level);
        setSearchRadiusMeters(option.meters);
        setAppliedBounds(null);
        setMapAreaSearch(true);
        setMapMoved(false);
        setRadiusPickerOpen(false);
        setPendingLocation(null);
        setLocationMessage(`현재 위치 ${option.km}km 이내 시설을 가까운 지도 배율로 표시합니다.`);
    };

    return (
        <main className="hospital-page">
            <div className="hospital-page-inner">
                <header className="hospital-heading">
                    <span className="hospital-eyebrow">ANIMAL HOSPITAL</span>
                    <h1>가까운 동물병원을 지도에서 확인하세요</h1>
                    <p>
                        행정안전부 인허가 데이터의 영업 중 동물병원 정보를 제공합니다.
                        현재 위치는 지도 중심 이동에만 사용되며 서버로 전송하거나 저장하지 않습니다.
                    </p>
                </header>

                <div className="hospital-kind-tabs" aria-label="의료시설 종류">
                    <button type="button" className={facilityType === "HOSPITAL" ? "active" : ""} onClick={() => { setFacilityType("HOSPITAL"); setMapAreaSearch(false); setAppliedBounds(null); applyNextBoundsRef.current = true; setPage(1); }}>동물병원</button>
                    <button type="button" className={facilityType === "PHARMACY" ? "active" : ""} onClick={() => { setFacilityType("PHARMACY"); setMapAreaSearch(false); setAppliedBounds(null); applyNextBoundsRef.current = true; setPage(1); }}>동물약국</button>
                </div>

                <section className="hospital-controls" aria-label="동물병원 및 동물약국 검색">
                    <form className="hospital-search" onSubmit={searchByName}>
                        <select value={province} onChange={(event) => { setProvince(event.target.value); setDistrict(""); }} aria-label="시·도 선택">
                            <option value="">시·도 선택</option>
                            {Object.keys(DISTRICTS).map((item) => <option key={item} value={item}>{item}</option>)}
                        </select>
                        <select value={district} onChange={(event) => setDistrict(event.target.value === "전체" ? "" : event.target.value)} disabled={!province} aria-label="시·군·구 선택">
                            <option value="">시·군·구 선택</option>
                            {(DISTRICTS[province] ?? []).map((item) => <option key={item} value={item}>{item}</option>)}
                        </select>
                        <label htmlFor="hospital-name" className="sr-only">
                            병원명
                        </label>
                        <input
                            id="hospital-name"
                            value={nameInput}
                            onChange={(event) => setNameInput(event.target.value)}
                            placeholder="병원명 검색"
                            maxLength={100}
                        />
                        <button type="submit">검색</button>
                    </form>

                </section>

                {locationMessage && (
                    <p className="hospital-location-message">{locationMessage}</p>
                )}

                <section className="hospital-content">
                    <div className="hospital-map-panel">
                        <KakaoMap
                            center={center}
                            level={mapLevel}
                            markers={markers}
                            selectedMarkerId={selectedHospitalId}
                            onMarkerSelect={handleHospitalMarkerSelect}
                            onBoundsChange={handleBoundsChange}
                        />
                        {(loading || locationLoading) && (
                            <div className="hospital-map-loading" role="status">
                                <span aria-hidden="true">🐾</span>
                                <strong>{facilityType === "HOSPITAL" ? "가까운 동물병원을 찾고 있어요." : "가까운 동물약국을 찾고 있어요."}</strong>
                            </div>
                        )}
                        {mapMoved && draftBounds && (
                            <button
                                type="button"
                                className="hospital-search-area-button"
                                onClick={() => {
                                    setSearchRadiusMeters(null);
                                    setAppliedBounds(draftBounds);
                                    setMapMoved(false);
                                    setMapAreaSearch(true);
                                }}
                            >
                                이 지도 영역 병원 보기
                            </button>
                        )}
                    </div>

                <div className="hospital-map-summary">
                    <span className="hospital-location-hint">{locationMessage ?? "위치 사용에 동의하면 가까운 반려동물 병원·약국을 보여드려요."}</span>
                    <button type="button" className="hospital-location-button" onClick={moveToCurrentLocation} disabled={loading}>
                        내 위치로 찾기
                    </button>
                </div>
                </section>

                <section className="hospital-results">
                    <header className="hospital-results-heading">
                        <div>
                            <span className="hospital-results-eyebrow">SEARCH RESULTS</span>
                            <h2>{facilityType === "HOSPITAL" ? "동물병원" : "동물약국"} 목록</h2>
                        </div>
                        <p>시설 카드를 선택하면 지도 중심이 해당 위치로 이동합니다.</p>
                    </header>

                    <div className="hospital-list-panel">
                        {(loading || locationLoading) && (
                            <div className="hospital-state paw-loading"><span aria-hidden="true">🐾</span><strong>가까운 동물병원을 찾고 있어요.</strong></div>
                        )}

                        {!loading && error && (
                            <div className="hospital-state error">{error}</div>
                        )}

                        {!loading && !error && orderedHospitals.length === 0 && (
                            <div className="hospital-state">현재 지도 영역에 표시할 병원이 없습니다.</div>
                        )}

                        {!loading && !error && orderedHospitals.map((hospital) => (
                            <div
                                key={hospital.id}
                                id={`hospital-${hospital.id}`}
                                className={`hospital-card${selectedHospitalId === hospital.id ? " is-selected" : ""}`}
                                onClick={() => moveToHospital(hospital)}
                                role="button" tabIndex={0}
                            >
                                <div className="hospital-card-primary">
                                    <div className="hospital-card-title"><strong>{hospital.name}</strong><div><ReviewButton targetType={facilityType === "PHARMACY" ? "PHARMACY" : "HOSPITAL"} targetId={hospital.id} title={hospital.name}/><FavoriteButton targetType={facilityType === "PHARMACY" ? "PHARMACY" : "HOSPITAL"} targetId={hospital.id} title={hospital.name} address={hospital.address} /></div></div>
                                    <span>{hospital.address ?? "주소 정보 없음"}</span>
                                    <span>{hospital.telephone ?? "전화번호 정보 없음"}</span>
                                </div>
                                <div className="hospital-card-detail">
                                    <strong>상세정보</strong>
                                    <span>운영시간 · {hospital.operatingHours ?? "방문 전 확인 필요"}</span>
                                    <span>휴무일 · {hospital.closedDays ?? "방문 전 확인 필요"}</span>
                                    {hospital.parking && <span>주차 · {hospital.parking}</span>}
                                </div>
                            </div>
                        ))}
                    </div>
                    {!loading && !mapAreaSearch && totalPages > 1 && <nav className="facility-pagination hospital-list-pagination" aria-label="의료시설 목록 페이지">
                        <button type="button" onClick={() => setPage((value) => Math.max(1, value - 1))} disabled={page === 1}>이전</button>
                        <span>{page.toLocaleString()} / {totalPages.toLocaleString()}</span>
                        <button type="button" onClick={() => setPage((value) => Math.min(totalPages, value + 1))} disabled={page === totalPages}>다음</button>
                    </nav>}
                </section>

            </div>
            <RadiusPicker open={radiusPickerOpen} onClose={() => { setRadiusPickerOpen(false); setPendingLocation(null); setLocationLoading(false); }} onSelect={selectCurrentLocationRadius}/>
        </main>
    );
}
