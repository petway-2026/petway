import {
    type FormEvent,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";

import KakaoMap from "../../place/components/KakaoMap";
import type { MapBounds, MapCenter, MapMarker } from "../../place/types/map";
import { getHospitals } from "../api/hospitalApi";
import type { Hospital } from "../types/hospital";

import "../styles/hospital.css";

const KOREA_CENTER: MapCenter = {
    latitude: 36.35,
    longitude: 127.85,
};
const HOSPITAL_PAGE_SIZE = 20;

const REGIONS = [
    { label: "전국", value: "", center: KOREA_CENTER, level: 13 },
    { label: "서울", value: "서울특별시", center: { latitude: 37.5665, longitude: 126.978 }, level: 9 },
    { label: "경기", value: "경기도", center: { latitude: 37.4138, longitude: 127.5183 }, level: 11 },
    { label: "인천", value: "인천광역시", center: { latitude: 37.4563, longitude: 126.7052 }, level: 9 },
    { label: "부산", value: "부산광역시", center: { latitude: 35.1796, longitude: 129.0756 }, level: 9 },
] as const;

export default function HospitalPage() {
    const [hospitals, setHospitals] = useState<Hospital[]>([]);
    const [facilityType, setFacilityType] = useState<"HOSPITAL" | "PHARMACY">("HOSPITAL");
    const [page, setPage] = useState(1);
    const [totalCount, setTotalCount] = useState(0);
    const [region, setRegion] = useState("");
    const [nameInput, setNameInput] = useState("");
    const [hospitalName, setHospitalName] = useState("");
    const [center, setCenter] = useState<MapCenter>(KOREA_CENTER);
    const [mapLevel, setMapLevel] = useState(13);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [locationMessage, setLocationMessage] = useState<string | null>(null);
    const [currentLocation, setCurrentLocation] = useState<MapCenter | null>(null);
    const [draftBounds, setDraftBounds] = useState<MapBounds | null>(null);
    const [appliedBounds, setAppliedBounds] = useState<MapBounds | null>(null);
    const [mapMoved, setMapMoved] = useState(false);
    const [selectedHospitalId, setSelectedHospitalId] = useState<string | null>(null);
    const applyNextBoundsRef = useRef(true);

    useEffect(() => {
        let active = true;

        const loadHospitals = async () => {
            setLoading(true);
            setError(null);

            try {
                const response = await getHospitals(
                    region || undefined,
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
            } catch (requestError) {
                if (!active) {
                    return;
                }

                setHospitals([]);
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
    }, [region, hospitalName, facilityType, page]);

    const totalPages = Math.max(1, Math.ceil(totalCount / HOSPITAL_PAGE_SIZE));

    const displayedHospitals = useMemo(
        () => hospitals.filter((hospital) =>
            hospital.latitude !== null &&
            hospital.longitude !== null &&
            (appliedBounds === null || (
                hospital.latitude >= appliedBounds.south &&
                hospital.latitude <= appliedBounds.north &&
                hospital.longitude >= appliedBounds.west &&
                hospital.longitude <= appliedBounds.east
            ))),
        [appliedBounds, hospitals]
    );

    const orderedHospitals = useMemo(() => {
        if (selectedHospitalId === null) return displayedHospitals;
        const selected = displayedHospitals.find((hospital) => hospital.id === selectedHospitalId);
        return selected
            ? [selected, ...displayedHospitals.filter((hospital) => hospital.id !== selectedHospitalId)]
            : displayedHospitals;
    }, [displayedHospitals, selectedHospitalId]);

    const markers = useMemo<MapMarker[]>(
        () => [
            ...displayedHospitals
            .map((hospital) => ({
                id: hospital.id,
                title: hospital.name,
                latitude: hospital.latitude as number,
                longitude: hospital.longitude as number,
                kind: "hospital" as const,
            })),
            ...(currentLocation ? [{
                id: "current-location",
                title: "현재 위치",
                latitude: currentLocation.latitude,
                longitude: currentLocation.longitude,
                kind: "current-location" as const,
            }] : []),
        ],
        [currentLocation, displayedHospitals]
    );

    const handleBoundsChange = useCallback((bounds: MapBounds) => {
        setDraftBounds(bounds);
        if (applyNextBoundsRef.current) {
            setAppliedBounds(bounds);
            setMapMoved(false);
            applyNextBoundsRef.current = false;
            return;
        }
        setMapMoved(true);
    }, []);

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
        setHospitalName(nameInput.trim());
        setPage(1);
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
        if (!marker || marker.kind !== "hospital") {
            setSelectedHospitalId(null);
            return;
        }

        const hospital = hospitals.find((candidate) => candidate.id === String(marker.id));
        if (!hospital) return;

        setSelectedHospitalId(hospital.id);
        window.requestAnimationFrame(() => {
            document.getElementById(`hospital-${hospital.id}`)?.scrollIntoView({
                behavior: "smooth",
                block: "start",
            });
        });
    }, [hospitals]);

    const moveToCurrentLocation = () => {
        if (!navigator.geolocation) {
            setLocationMessage("이 브라우저에서는 현재 위치를 사용할 수 없습니다.");
            return;
        }

        setLocationMessage("현재 위치를 확인하고 있습니다.");

        navigator.geolocation.getCurrentPosition(
            (position) => {
                // GPS stays in the browser. It is used only as the map center
                // and is never included in the hospital API request.
                const location = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                };
                applyNextBoundsRef.current = true;
                setCurrentLocation(location);
                setCenter(location);
                setMapLevel(5);
                setLocationMessage("현재 위치를 지도 중심으로 표시했습니다.");
            },
            () => {
                setLocationMessage("현재 위치를 확인할 수 없습니다.");
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 60000,
            }
        );
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
                    <button type="button" className={facilityType === "HOSPITAL" ? "active" : ""} onClick={() => { setFacilityType("HOSPITAL"); setPage(1); }}>동물병원</button>
                    <button type="button" className={facilityType === "PHARMACY" ? "active" : ""} onClick={() => { setFacilityType("PHARMACY"); setPage(1); }}>동물약국</button>
                </div>

                <section className="hospital-controls" aria-label="동물병원 및 동물약국 검색">
                    <div className="hospital-region-filter">
                        {REGIONS.map((item) => (
                            <button
                                key={item.label}
                                type="button"
                                className={region === item.value ? "active" : ""}
                                onClick={() => selectRegion(item)}
                            >
                                {item.label}
                            </button>
                        ))}
                    </div>

                    <form className="hospital-search" onSubmit={searchByName}>
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

                    <button
                        type="button"
                        className="hospital-location-button"
                        onClick={moveToCurrentLocation}
                    >
                        내 위치로 지도 이동
                    </button>
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
                        {mapMoved && draftBounds && (
                            <button
                                type="button"
                                className="hospital-search-area-button"
                                onClick={() => {
                                    setAppliedBounds(draftBounds);
                                    setMapMoved(false);
                                }}
                            >
                                이 지도 영역 병원 보기
                            </button>
                        )}
                    </div>

                    <div className="hospital-map-summary">
                        지도와 목록에 표시된 {facilityType === "HOSPITAL" ? "동물병원" : "동물약국"} {displayedHospitals.length.toLocaleString()}곳 · 전체 {totalCount.toLocaleString()}곳
                    </div>
                    {!loading && totalPages > 1 && <nav className="facility-pagination" aria-label="의료시설 페이지">
                        <button type="button" onClick={() => setPage((value) => Math.max(1, value - 1))} disabled={page === 1}>이전</button>
                        <span>{page.toLocaleString()} / {totalPages.toLocaleString()}</span>
                        <button type="button" onClick={() => setPage((value) => Math.min(totalPages, value + 1))} disabled={page === totalPages}>다음</button>
                    </nav>}
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
                        {loading && (
                            <div className="hospital-state paw-loading"><span aria-hidden="true">🐾</span><strong>가까운 동물병원을 찾고 있어요.</strong></div>
                        )}

                        {!loading && error && (
                            <div className="hospital-state error">{error}</div>
                        )}

                        {!loading && !error && displayedHospitals.length === 0 && (
                            <div className="hospital-state">현재 지도 영역에 표시할 병원이 없습니다.</div>
                        )}

                        {!loading && !error && orderedHospitals.map((hospital) => (
                            <button
                                key={hospital.id}
                                type="button"
                                id={`hospital-${hospital.id}`}
                                className={`hospital-card${selectedHospitalId === hospital.id ? " is-selected" : ""}`}
                                onClick={() => moveToHospital(hospital)}
                                disabled={hospital.latitude === null || hospital.longitude === null}
                            >
                                <div className="hospital-card-primary">
                                    <strong>{hospital.name}</strong>
                                    <span>{hospital.address ?? "주소 정보 없음"}</span>
                                    <span>{hospital.telephone ?? "전화번호 정보 없음"}</span>
                                </div>
                                <div className="hospital-card-detail">
                                    <strong>상세정보</strong>
                                    <span>운영시간 · {hospital.operatingHours ?? "방문 전 확인 필요"}</span>
                                    <span>휴무일 · {hospital.closedDays ?? "방문 전 확인 필요"}</span>
                                    {hospital.parking && <span>주차 · {hospital.parking}</span>}
                                </div>
                            </button>
                        ))}
                    </div>
                </section>

            </div>
        </main>
    );
}
