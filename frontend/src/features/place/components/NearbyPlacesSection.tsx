import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
    getPlaceCompatibility,
    getPlacePetDetail,
    PlaceCompatibilityApiError,
    searchAllNearbyPlaces,
    type CompatibilityStatus,
    type PlaceCompatibility,
    type PlacePetDetail,
    type PlaceSummary,
} from "../api/placeApi";
import KakaoMap from "./KakaoMap";
import PlaceWeatherPanel from "./PlaceWeatherPanel";
import type { MapBounds, MapCenter, MapMarker } from "../types/map";
import type { SelectedPlace } from "../types/place";
import { resolveWeatherRegion } from "../utils/resolveWeatherRegion";
import FavoriteButton from "../../../shared/components/FavoriteButton";
import ReviewButton from "../../../shared/components/ReviewButton";
import RadiusPicker, { RADIUS_OPTIONS, type RadiusOption } from "../../../shared/components/RadiusPicker";

import "../styles/nearby.css";

const DEFAULT_LOCATION: MapCenter = {
    latitude: 37.5665,
    longitude: 126.978,
};

const INITIAL_LOCATION_MESSAGE =
    "위치 사용에 동의하면 가까운 반려동물 동반 장소를 보여드려요.";

const CONTENT_TYPE_NAMES: Record<string, string> = {
    "12": "관광지",
    "14": "문화시설",
    "15": "축제·행사",
    "28": "레포츠",
    "32": "숙박",
    "38": "쇼핑",
    "39": "음식점",
};

const COMPATIBILITY_PRESENTATION: Record<CompatibilityStatus, {
    label: string;
    className: string;
}> = {
    AVAILABLE: {
        label: "함께 방문할 수 있어요",
        className: "available",
    },
    CONDITIONAL: {
        label: "조건을 지키면 함께 방문할 수 있어요",
        className: "conditional",
    },
    NEED_CONFIRMATION: {
        label: "방문 전 확인이 필요해요",
        className: "confirmation",
    },
    UNAVAILABLE: {
        label: "현재 조건으로는 동반이 어려워요",
        className: "unavailable",
    },
};

function getRadiusPolicy(location: MapCenter): { radius: number; label: string } {
    const { latitude, longitude } = location;
    const isSeoul = latitude >= 37.41 && latitude <= 37.72
        && longitude >= 126.73 && longitude <= 127.27;
    if (isSeoul) return { radius: 10000, label: "서울 10km" };

    const isCapitalArea = latitude >= 36.85 && latitude <= 38.30
        && longitude >= 126.05 && longitude <= 127.85;
    if (isCapitalArea) return { radius: 20000, label: "수도권 20km" };

    return { radius: 30000, label: "타지역 30km" };
}

function distanceInMeters(from: MapCenter, place: PlaceSummary): number | null {
    if (place.latitude === null || place.longitude === null) return null;
    const radians = (degree: number) => degree * Math.PI / 180;
    const latitudeDelta = radians(place.latitude - from.latitude);
    const longitudeDelta = radians(place.longitude - from.longitude);
    const a = Math.sin(latitudeDelta / 2) ** 2
        + Math.cos(radians(from.latitude))
        * Math.cos(radians(place.latitude))
        * Math.sin(longitudeDelta / 2) ** 2;
    return Math.round(6371000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
}

function formatDistance(distance: number | null): string {
    if (distance === null) return "거리 정보 없음";
    return distance < 1000 ? `${distance}m` : `${(distance / 1000).toFixed(1)}km`;
}

interface NearbyPlacesSectionProps {
    showFullMapButton?: boolean;
    enableWeatherSelection?: boolean;
}

export default function NearbyPlacesSection({
    showFullMapButton = true,
    enableWeatherSelection = false,
}: NearbyPlacesSectionProps) {
    const navigate = useNavigate();
    const [currentLocation, setCurrentLocation] = useState(DEFAULT_LOCATION);
    // Programmatic map moves and the center reported by a user drag must not
    // share one state value. Feeding the reported center straight back into
    // KakaoMap can make the map snap while it is being dragged.
    const [mapTargetCenter, setMapTargetCenter] = useState(DEFAULT_LOCATION);
    const [mapCenter, setMapCenter] = useState(DEFAULT_LOCATION);
    const [searchOrigin, setSearchOrigin] = useState(DEFAULT_LOCATION);
    const [hasCurrentLocation, setHasCurrentLocation] = useState(false);
    const [mapMoved, setMapMoved] = useState(false);
    const [draftBounds, setDraftBounds] = useState<MapBounds | null>(null);
    const [appliedBounds, setAppliedBounds] = useState<MapBounds | null>(null);
    const [radiusPolicy, setRadiusPolicy] = useState<{ radius: number; label: string }>({ radius: RADIUS_OPTIONS[2].meters, label: "10km" });
    const [mapLevel, setMapLevel] = useState<number>(RADIUS_OPTIONS[2].level);
    const [radiusPickerOpen, setRadiusPickerOpen] = useState(false);
    const [pendingLocation, setPendingLocation] = useState<MapCenter | null>(null);
    const [places, setPlaces] = useState<PlaceSummary[]>([]);
    const [expandedPlaceId, setExpandedPlaceId] = useState<string | null>(null);
    const [detail, setDetail] = useState<PlacePetDetail | null>(null);
    const [detailLoading, setDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState("");
    const [compatibility, setCompatibility] = useState<PlaceCompatibility | null>(null);
    const [compatibilityLoading, setCompatibilityLoading] = useState(false);
    const [compatibilityError, setCompatibilityError] = useState("");
    const [selectedPlace, setSelectedPlace] = useState<SelectedPlace | null>(null);
    const [selectedPlaceId, setSelectedPlaceId] = useState<string | null>(null);
    const [selectedType, setSelectedType] = useState("all");
    const [locationLoading, setLocationLoading] = useState(false);
    const [locationMessage, setLocationMessage] = useState(INITIAL_LOCATION_MESSAGE);
    const [mapAreaSearch, setMapAreaSearch] = useState(false);
    const detailRequestRef = useRef<{ id: number; controller: AbortController } | null>(null);
    const compatibilityRequestRef = useRef<AbortController | null>(null);
    const detailRequestIdRef = useRef(0);
    const locationStatusMessageRef = useRef(INITIAL_LOCATION_MESSAGE);
    const weatherMessageActiveRef = useRef(false);

    const updateLocationMessage = useCallback((message: string) => {
        locationStatusMessageRef.current = message;
        weatherMessageActiveRef.current = false;
        setLocationMessage(message);
    }, []);

    const showWeatherSelectionMessage = useCallback((message: string) => {
        weatherMessageActiveRef.current = true;
        setLocationMessage(message);
    }, []);

    const restoreLocationMessage = useCallback(() => {
        if (!weatherMessageActiveRef.current) return;
        weatherMessageActiveRef.current = false;
        setLocationMessage(locationStatusMessageRef.current);
    }, []);

    const closePlaceExpansion = useCallback(() => {
        detailRequestRef.current?.controller.abort();
        detailRequestRef.current = null;
        setExpandedPlaceId(null);
        setDetail(null);
        setDetailLoading(false);
        setDetailError("");
        compatibilityRequestRef.current?.abort();
        compatibilityRequestRef.current = null;
        setCompatibility(null);
        setCompatibilityLoading(false);
        setCompatibilityError("");
        setSelectedPlace(null);
        setSelectedPlaceId(null);
        restoreLocationMessage();
    }, [restoreLocationMessage]);

    useEffect(() => () => {
        detailRequestRef.current?.controller.abort();
        compatibilityRequestRef.current?.abort();
    }, []);

    const getCurrentLocation = () => {
        setMapAreaSearch(false);
        if (!navigator.geolocation) {
            updateLocationMessage("이 브라우저에서는 현재 위치를 사용할 수 없습니다.");
            return;
        }

        setLocationLoading(true);
        updateLocationMessage("현재 위치를 확인하고 있어요.");

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                const location = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                };
                setPendingLocation(location);
                setLocationLoading(false);
                setRadiusPickerOpen(true);
                updateLocationMessage("검색할 반경을 선택해주세요.");
            },
            (error) => {
                setLocationLoading(false);
                updateLocationMessage(
                    error.code === error.PERMISSION_DENIED
                        ? "위치 권한을 허용하면 가까운 순으로 장소를 확인할 수 있어요."
                        : "현재 위치를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요."
                );
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
        );
    };

    const searchAt = async (location: MapCenter, areaSearch = false, radiusOption?: RadiusOption, targetBounds?: MapBounds | null) => {
        setMapAreaSearch(areaSearch);
        setLocationLoading(true);
        updateLocationMessage("선택한 지도 영역의 장소를 찾고 있어요.");
        try {
            const policy = radiusOption
                ? { radius: radiusOption.meters, label: `${radiusOption.km}km` }
                : radiusPolicy;
            const response = await searchAllNearbyPlaces(
                location.latitude,
                location.longitude,
                policy.radius,
                (partial) => setPlaces([...partial.items].sort((left, right) =>
                    (distanceInMeters(location, left) ?? Number.MAX_VALUE)
                    - (distanceInMeters(location, right) ?? Number.MAX_VALUE)))
            );
            const sortedPlaces = [...response.items].sort((left, right) =>
                (distanceInMeters(location, left) ?? Number.MAX_VALUE)
                - (distanceInMeters(location, right) ?? Number.MAX_VALUE)
            );
            setSearchOrigin(location);
            setPlaces(sortedPlaces);
            setAppliedBounds(areaSearch ? targetBounds ?? null : null);
            closePlaceExpansion();
            setSelectedType("all");
            setMapMoved(false);
            setRadiusPolicy(policy);
            updateLocationMessage(
                sortedPlaces.length > 0
                    ? `${areaSearch ? "현재 지도 영역" : policy.label}에서 총 ${sortedPlaces.length}곳을 찾았어요. 가까운 순으로 확인해보세요.`
                    : `${policy.label} 범위에서 등록된 동반 장소를 찾지 못했어요.`
            );
        } catch (cause) {
            updateLocationMessage(cause instanceof Error ? cause.message : "주변 장소를 불러오지 못했습니다.");
        } finally {
            setLocationLoading(false);
        }
    };

    const selectCurrentLocationRadius = (option: RadiusOption) => {
        if (!pendingLocation) return;
        const location = pendingLocation;
        setRadiusPickerOpen(false);
        setPendingLocation(null);
        setCurrentLocation(location);
        setMapTargetCenter(location);
        setMapCenter(location);
        setSearchOrigin(location);
        setHasCurrentLocation(true);
        setMapLevel(option.level);
        setAppliedBounds(null);
        void searchAt(location, false, option);
    };

    const handleMapCenterChange = useCallback((center: MapCenter) => {
        setMapCenter(center);
        const moved =
            Math.abs(center.latitude - searchOrigin.latitude) > 0.0001 ||
            Math.abs(center.longitude - searchOrigin.longitude) > 0.0001;
        setMapMoved(moved);
    }, [searchOrigin]);

    const areaPlaces = useMemo(
        () => appliedBounds === null ? places : places.filter((place) =>
            place.latitude !== null && place.longitude !== null
            && place.latitude >= appliedBounds.south && place.latitude <= appliedBounds.north
            && place.longitude >= appliedBounds.west && place.longitude <= appliedBounds.east
        ),
        [appliedBounds, places]
    );

    const availableTypes = useMemo(
        () => Array.from(new Set(areaPlaces.map((place) => place.contentTypeId))),
        [areaPlaces]
    );

    const filteredPlaces = useMemo(
        () => selectedType === "all"
            ? areaPlaces
            : areaPlaces.filter((place) => place.contentTypeId === selectedType),
        [areaPlaces, selectedType]
    );
    const pagedPlaces = filteredPlaces;

    const orderedPlaces = useMemo(() => {
        const located = pagedPlaces.filter((place) => place.latitude !== null && place.longitude !== null);
        if (selectedPlaceId === null) return located;
        const selected = located.find((place) => place.contentId === selectedPlaceId);
        return selected
            ? [selected, ...located.filter((place) => place.contentId !== selectedPlaceId)]
            : located;
    }, [pagedPlaces, selectedPlaceId]);

    const markers = useMemo<MapMarker[]>(() => [
        ...pagedPlaces
            .filter((place) => place.latitude !== null && place.longitude !== null)
            .map((place) => ({
                id: place.contentId,
                title: place.title,
                latitude: place.latitude as number,
                longitude: place.longitude as number,
                kind: "place" as const,
            })),
        ...(hasCurrentLocation ? [{
            id: "current",
            title: "현재 위치",
            latitude: currentLocation.latitude,
            longitude: currentLocation.longitude,
            kind: "current-location" as const,
        }] : []),
    ], [currentLocation, pagedPlaces, hasCurrentLocation]);

    const selectPlace = useCallback((place: PlaceSummary) => {
        if (!enableWeatherSelection) return;
        if (place.latitude === null || place.longitude === null) {
            setSelectedPlace(null);
            showWeatherSelectionMessage("선택한 장소의 좌표 정보가 없어 날씨를 조회할 수 없어요.");
            return;
        }

        const region = resolveWeatherRegion(place.areaCode, place.sigunguCode);
        if (!region) {
            setSelectedPlace(null);
            showWeatherSelectionMessage("선택한 장소의 지역 정보가 없어 날씨를 조회할 수 없어요.");
            return;
        }

        restoreLocationMessage();

        setSelectedPlace({
            id: place.contentId,
            contentId: place.contentId,
            contentTypeId: place.contentTypeId,
            title: place.title,
            address: place.address ?? "주소 정보가 제공되지 않습니다.",
            latitude: place.latitude,
            longitude: place.longitude,
            kind: "place",
            region,
            thumbnailUrl: place.thumbnailUrl ?? undefined,
        });
    }, [enableWeatherSelection, restoreLocationMessage, showWeatherSelectionMessage]);

    const handleMarkerSelect = useCallback((marker: MapMarker | null) => {
        if (!marker || marker.kind !== "place") {
            closePlaceExpansion();
            return;
        }

        const place = places.find((candidate) => candidate.contentId === String(marker.id));
        if (place) {
            closePlaceExpansion();
            setSelectedPlaceId(place.contentId);
            selectPlace(place);
            window.requestAnimationFrame(() => {
                document.getElementById(`nearby-place-${place.contentId}`)?.scrollIntoView({
                    behavior: "smooth",
                    block: "start",
                });
            });
        }
    }, [closePlaceExpansion, places, selectPlace]);

    const loadPlaceDetail = async (place: PlaceSummary) => {
        detailRequestRef.current?.controller.abort();

        const controller = new AbortController();
        const requestId = detailRequestIdRef.current + 1;
        detailRequestIdRef.current = requestId;
        detailRequestRef.current = { id: requestId, controller };

        setDetail(null);
        setDetailError("");
        setDetailLoading(true);

        try {
            const response = await getPlacePetDetail(place.contentId, controller.signal);
            if (detailRequestRef.current?.id === requestId) {
                setDetail(response);
            }
        } catch (cause) {
            if (controller.signal.aborted) return;
            if (detailRequestRef.current?.id === requestId) {
                setDetailError(
                    cause instanceof Error
                        ? cause.message
                        : "반려동물 동반 상세정보를 불러오지 못했습니다."
                );
            }
        } finally {
            if (detailRequestRef.current?.id === requestId) {
                detailRequestRef.current = null;
                setDetailLoading(false);
            }
        }
    };

    const toggleDetail = (place: PlaceSummary) => {
        if (expandedPlaceId === place.contentId) {
            closePlaceExpansion();
            return;
        }

        compatibilityRequestRef.current?.abort();
        compatibilityRequestRef.current = null;
        setCompatibility(null);
        setCompatibilityLoading(false);
        setCompatibilityError("");
        setExpandedPlaceId(place.contentId);
        selectPlace(place);
        void loadPlaceDetail(place);
        void loadCompatibility(place);
    };

    const loadCompatibility = async (place: PlaceSummary) => {
        compatibilityRequestRef.current?.abort();
        const controller = new AbortController();
        compatibilityRequestRef.current = controller;

        setCompatibility(null);
        setCompatibilityError("");
        setCompatibilityLoading(true);

        try {
            const response = await getPlaceCompatibility(
                place.contentId,
                controller.signal
            );
            if (!controller.signal.aborted) {
                detailRequestRef.current?.controller.abort();
                detailRequestRef.current = null;
                setCompatibility(response);
                setDetail(response.officialInformation);
                setDetailLoading(false);
                setDetailError("");
            }
        } catch (cause) {
            if (controller.signal.aborted) return;

            if (cause instanceof PlaceCompatibilityApiError
                && (cause.status === 401 || cause.status === 403)) {
                setCompatibilityError(
                    "로그인 정보가 만료되었습니다. 다시 로그인한 뒤 확인해 주세요."
                );
            } else {
                setCompatibilityError(
                    cause instanceof Error
                        ? cause.message
                        : "내 반려견의 동반 가능 여부를 확인하지 못했습니다."
                );
            }
        } finally {
            if (!controller.signal.aborted) {
                compatibilityRequestRef.current = null;
                setCompatibilityLoading(false);
            }
        }
    };

    return (
        <section className="nearby-section">
            <div className="nearby-header">
                <div className="nearby-title-area">
                    <span className="nearby-eyebrow">NEARBY</span>
                    <h2 className="nearby-title">내 주변 동반 장소</h2>
                    <p className="nearby-description">
                        위치 동의 후 한국관광공사와 한국문화정보원의 반려동물 동반 장소를 가까운 순으로 확인하세요.
                    </p>
                </div>
                {showFullMapButton && (
                    <button type="button" className="nearby-map-button" onClick={() => navigate("/places")}>
                        전체 지도 보기 <span aria-hidden="true">→</span>
                    </button>
                )}
            </div>

            <div className="nearby-map-card">
                <div className="nearby-map">
                    <KakaoMap
                        center={mapTargetCenter}
                        markers={markers}
                        level={mapLevel}
                        selectedMarkerId={selectedPlaceId}
                        onMarkerSelect={handleMarkerSelect}
                        onCenterChange={handleMapCenterChange}
                        onBoundsChange={setDraftBounds}
                    />
                    {locationLoading && <div className="nearby-paw-loading" role="status"><span aria-hidden="true">🐾</span><strong>주변 장소를 찾고 있어요.</strong></div>}
                    {mapMoved && (
                        <button
                            type="button"
                            className="nearby-search-area-button"
                            onClick={() => void searchAt(mapCenter, true, undefined, draftBounds)}
                            disabled={locationLoading}
                        >
                            이 지역 검색
                        </button>
                    )}
                </div>
                <div className="nearby-map-status">
                    <div className="nearby-status-left">
                        <span className="nearby-status-label">
                            {locationLoading ? "위치 검색 중" : "검색 안내"}
                        </span>
                        <span>{locationMessage}</span>
                    </div>
                    <button
                        type="button"
                        className="nearby-location-refresh"
                        onClick={getCurrentLocation}
                        disabled={locationLoading}
                    >
                        {locationLoading ? "찾는 중…" : places.length > 0 ? "위치 다시 찾기" : "내 위치로 찾기"}
                    </button>
                </div>
            </div>

            {filteredPlaces.length > 0 && (
                <>
                    <div className="nearby-place-filters" aria-label="장소 종류 필터">
                        <span className="nearby-radius-label">검색 범위 · {mapAreaSearch ? "현재 지도 영역" : radiusPolicy.label}</span>
                        <button
                            type="button"
                            className={selectedType === "all" ? "active" : ""}
                            onClick={() => {
                                setSelectedType("all");
                                closePlaceExpansion();
                            }}
                        >
                            전체 <span>{areaPlaces.length}</span>
                        </button>
                        {availableTypes.map((contentTypeId) => (
                            <button
                                type="button"
                                key={contentTypeId}
                                className={selectedType === contentTypeId ? "active" : ""}
                                onClick={() => {
                                    setSelectedType(contentTypeId);
                                    closePlaceExpansion();
                                }}
                            >
                                {CONTENT_TYPE_NAMES[contentTypeId] ?? "기타"}
                                <span>{areaPlaces.filter((place) => place.contentTypeId === contentTypeId).length}</span>
                            </button>
                        ))}
                    </div>
                    <div className="nearby-place-grid">
                    {orderedPlaces.map((place) => {
                        const isSelected = selectedPlaceId === place.contentId;
                        const weatherSelected = selectedPlace?.contentId === place.contentId;
                        const detailExpanded = expandedPlaceId === place.contentId;
                        const hasPetDetail = detail !== null && Boolean(
                            detail.accompanimentType
                            || detail.allowedCapacity
                            || detail.requiredItems
                            || detail.additionalInformation
                            || detail.safetyNotice
                        );

                        return (
                            <div
                                className="nearby-place-row"
                                key={place.contentId}
                                id={`nearby-place-${place.contentId}`}
                            >
                                <article className={`nearby-place-card${weatherSelected ? " has-weather" : ""}${isSelected ? " is-selected" : ""}`}>
                                    <div className="nearby-place-image">
                                        {place.thumbnailUrl ? (
                                            <img src={place.thumbnailUrl} alt="" loading="lazy" />
                                        ) : (
                                            <span>PetWay</span>
                                        )}
                                    </div>
                                    <div className="nearby-place-body">
                                        <div className="nearby-place-meta">
                                            <span>{CONTENT_TYPE_NAMES[place.contentTypeId] ?? "동반 장소"}</span>
                                            <strong>{formatDistance(distanceInMeters(searchOrigin, place))}</strong>
                                        </div>
                                        <div className="nearby-place-title"><h3>{place.title}</h3><div><ReviewButton targetType="PLACE" targetId={place.contentId} title={place.title}/><FavoriteButton targetType="PLACE" targetId={place.contentId} title={place.title} address={place.address} thumbnailUrl={place.thumbnailUrl} /></div></div>
                                        <p>{place.address ?? "주소 정보가 제공되지 않습니다."}</p>
                                        {place.operatingHours && <small>운영시간 · {place.operatingHours}</small>}
                                        {place.closedDays && <small>휴무일 · {place.closedDays}</small>}
                                        <small>출처 · {place.source ?? "한국관광공사"}</small>
                                        <button
                                            type="button"
                                            className="nearby-detail-button"
                                            onClick={() => toggleDetail(place)}
                                            aria-expanded={detailExpanded}
                                        >
                                            {detailExpanded ? "상세정보 닫기" : "마이펫 기준 동반 가능 여부"}
                                        </button>
                                        {detailExpanded && !compatibility && !compatibilityLoading && (
                                            <button
                                                type="button"
                                                className="nearby-compatibility-button"
                                                onClick={() => void loadCompatibility(place)}
                                                disabled={compatibilityLoading}
                                            >
                                                {compatibilityLoading
                                                    ? "마이펫 기준으로 확인 중…"
                                                    : "마이펫 기준 다시 확인"}
                                            </button>
                                        )}
                                        {detailExpanded && compatibilityError && !compatibilityLoading && (
                                            <div className="nearby-compatibility-error" role="alert">
                                                <span>{compatibilityError}</span>
                                                <button
                                                    type="button"
                                                    onClick={() => void loadCompatibility(place)}
                                                >
                                                    다시 시도
                                                </button>
                                            </div>
                                        )}
                                        {detailExpanded && compatibility && !compatibilityLoading && !compatibilityError && (() => {
                                            const presentation = COMPATIBILITY_PRESENTATION[
                                                compatibility.petwayAssessment.status
                                            ];
                                            const assessment = compatibility.petwayAssessment;

                                            return (
                                                <section className={`nearby-compatibility-result ${presentation.className}`}>
                                                    <span className="nearby-compatibility-eyebrow">
                                                        PETWAY 판정
                                                    </span>
                                                    <h4>
                                                        {compatibility.petName} 기준 · {presentation.label}
                                                    </h4>

                                                    {assessment.reasons.length > 0 && (
                                                        <div className="nearby-compatibility-group">
                                                            <strong>
                                                                {assessment.needsConfirmation
                                                                    ? "확인이 필요해요"
                                                                    : "판정 이유"}
                                                            </strong>
                                                            <ul>
                                                                {assessment.reasons.map((reason) => (
                                                                    <li key={reason}>{reason}</li>
                                                                ))}
                                                            </ul>
                                                        </div>
                                                    )}

                                                    {assessment.requirements.length > 0 && (
                                                        <div className="nearby-compatibility-group">
                                                            <strong>준비해주세요</strong>
                                                            <ul>
                                                                {assessment.requirements.map((requirement, index) => (
                                                                    <li key={`${requirement.type}-${requirement.scope}-${index}`}>
                                                                        {requirement.label}
                                                                    </li>
                                                                ))}
                                                            </ul>
                                                        </div>
                                                    )}
                                                </section>
                                            );
                                        })()}
                                        {detailExpanded && detailLoading && (
                                            <div className="nearby-pet-detail-status" role="status">
                                                동반 상세정보를 불러오고 있어요.
                                            </div>
                                        )}
                                        {detailExpanded && detailError && !detailLoading && (
                                            <div className="nearby-pet-detail-error" role="alert">
                                                <span>{detailError}</span>
                                                <button
                                                    type="button"
                                                    onClick={() => void loadPlaceDetail(place)}
                                                >
                                                    다시 시도
                                                </button>
                                            </div>
                                        )}
                                        {detailExpanded && detail && hasPetDetail && !detailLoading && !detailError && (
                                            <section className="nearby-official-information">
                                                <strong>공식 안내 · {place.source ?? "한국관광공사"} 제공</strong>
                                                <dl className="nearby-pet-detail">
                                                    {detail.accompanimentType && <><dt>동반 유형</dt><dd>{detail.accompanimentType}</dd></>}
                                                    {detail.allowedCapacity && <><dt>동반 가능 동물</dt><dd>{detail.allowedCapacity}</dd></>}
                                                    {detail.requiredItems && <><dt>필요사항</dt><dd>{detail.requiredItems}</dd></>}
                                                    {detail.additionalInformation && <><dt>이용 안내</dt><dd>{detail.additionalInformation}</dd></>}
                                                    {detail.safetyNotice && <><dt>안전 유의사항</dt><dd>{detail.safetyNotice}</dd></>}
                                                </dl>
                                            </section>
                                        )}
                                        {detailExpanded && detail && !hasPetDetail && !detailLoading && !detailError && (
                                            <p className="nearby-pet-detail-empty">
                                                제공된 반려동물 동반 상세정보가 없습니다.
                                            </p>
                                        )}
                                    </div>

                                    {weatherSelected && (
                                        <PlaceWeatherPanel
                                            place={selectedPlace}
                                            onClose={closePlaceExpansion}
                                        />
                                    )}
                                </article>
                            </div>
                        );
                    })}
                    </div>
                </>
            )}
            <RadiusPicker open={radiusPickerOpen} onClose={() => { setRadiusPickerOpen(false); setPendingLocation(null); }} onSelect={selectCurrentLocationRadius}/>
        </section>
    );
}
