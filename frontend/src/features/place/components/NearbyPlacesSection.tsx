import { useCallback, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
    getPlacePetDetail,
    searchNearbyPlaces,
    type PlacePetDetail,
    type PlaceSummary,
} from "../api/placeApi";
import KakaoMap from "./KakaoMap";
import type { MapCenter, MapMarker } from "../types/map";

import "../styles/nearby.css";

const DEFAULT_LOCATION: MapCenter = {
    latitude: 37.5665,
    longitude: 126.978,
};

const CONTENT_TYPE_NAMES: Record<string, string> = {
    "12": "관광지",
    "14": "문화시설",
    "15": "축제·행사",
    "28": "레포츠",
    "32": "숙박",
    "38": "쇼핑",
    "39": "음식점",
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
}

export default function NearbyPlacesSection({
    showFullMapButton = true,
}: NearbyPlacesSectionProps) {
    const navigate = useNavigate();
    const [currentLocation, setCurrentLocation] = useState(DEFAULT_LOCATION);
    const [mapCenter, setMapCenter] = useState(DEFAULT_LOCATION);
    const [searchOrigin, setSearchOrigin] = useState(DEFAULT_LOCATION);
    const [hasCurrentLocation, setHasCurrentLocation] = useState(false);
    const [mapMoved, setMapMoved] = useState(false);
    const [radiusPolicy, setRadiusPolicy] = useState(getRadiusPolicy(DEFAULT_LOCATION));
    const [places, setPlaces] = useState<PlaceSummary[]>([]);
    const [details, setDetails] = useState<Record<string, PlacePetDetail>>({});
    const [detailLoadingId, setDetailLoadingId] = useState<string | null>(null);
    const [selectedType, setSelectedType] = useState("all");
    const [locationLoading, setLocationLoading] = useState(false);
    const [locationMessage, setLocationMessage] = useState(
        "위치 사용에 동의하면 가까운 반려동물 동반 장소를 보여드려요."
    );

    const getCurrentLocation = () => {
        if (!navigator.geolocation) {
            setLocationMessage("이 브라우저에서는 현재 위치를 사용할 수 없습니다.");
            return;
        }

        setLocationLoading(true);
        setLocationMessage("현재 위치와 가까운 장소를 찾고 있어요.");

        navigator.geolocation.getCurrentPosition(
            async (position) => {
                const location = {
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                };
                setCurrentLocation(location);
                setMapCenter(location);
                setSearchOrigin(location);
                setHasCurrentLocation(true);

                try {
                    const policy = getRadiusPolicy(location);
                    const response = await searchNearbyPlaces(
                        location.latitude,
                        location.longitude,
                        policy.radius
                    );
                    const sortedPlaces = [...response.items].sort((left, right) =>
                        (distanceInMeters(location, left) ?? Number.MAX_VALUE)
                        - (distanceInMeters(location, right) ?? Number.MAX_VALUE)
                    );
                    setPlaces(sortedPlaces);
                    setSelectedType("all");
                    setMapMoved(false);
                    setRadiusPolicy(policy);
                    setLocationMessage(
                        sortedPlaces.length > 0
                            ? `${policy.label} 범위에서 가까운 순으로 ${sortedPlaces.length}곳을 찾았어요.`
                            : `${policy.label} 범위에서 등록된 반려동물 동반 장소를 찾지 못했어요.`
                    );
                } catch (cause) {
                    setPlaces([]);
                    setLocationMessage(
                        cause instanceof Error
                            ? cause.message
                            : "주변 장소를 불러오지 못했습니다."
                    );
                } finally {
                    setLocationLoading(false);
                }
            },
            (error) => {
                setLocationLoading(false);
                setLocationMessage(
                    error.code === error.PERMISSION_DENIED
                        ? "위치 권한을 허용하면 가까운 순으로 장소를 확인할 수 있어요."
                        : "현재 위치를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요."
                );
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
        );
    };

    const searchAt = async (location: MapCenter) => {
        setLocationLoading(true);
        setLocationMessage("선택한 지도 영역의 장소를 찾고 있어요.");
        try {
            const policy = getRadiusPolicy(location);
            const response = await searchNearbyPlaces(
                location.latitude,
                location.longitude,
                policy.radius
            );
            const sortedPlaces = [...response.items].sort((left, right) =>
                (distanceInMeters(location, left) ?? Number.MAX_VALUE)
                - (distanceInMeters(location, right) ?? Number.MAX_VALUE)
            );
            setSearchOrigin(location);
            setPlaces(sortedPlaces);
            setSelectedType("all");
            setMapMoved(false);
            setRadiusPolicy(policy);
            setLocationMessage(
                sortedPlaces.length > 0
                    ? `${policy.label} 범위에서 가까운 순으로 ${sortedPlaces.length}곳을 찾았어요.`
                    : `${policy.label} 범위에서 등록된 동반 장소를 찾지 못했어요.`
            );
        } catch (cause) {
            setLocationMessage(cause instanceof Error ? cause.message : "주변 장소를 불러오지 못했습니다.");
        } finally {
            setLocationLoading(false);
        }
    };

    const handleMapCenterChange = useCallback((center: MapCenter) => {
        setMapCenter(center);
        const moved =
            Math.abs(center.latitude - searchOrigin.latitude) > 0.0001 ||
            Math.abs(center.longitude - searchOrigin.longitude) > 0.0001;
        setMapMoved(moved);
    }, [searchOrigin]);

    const availableTypes = useMemo(
        () => Array.from(new Set(places.map((place) => place.contentTypeId))),
        [places]
    );

    const filteredPlaces = useMemo(
        () => selectedType === "all"
            ? places
            : places.filter((place) => place.contentTypeId === selectedType),
        [places, selectedType]
    );

    const markers: MapMarker[] = [
        ...(filteredPlaces
            .filter((place) => place.latitude !== null && place.longitude !== null)
            .map((place) => ({
                id: place.contentId,
                title: place.title,
                latitude: place.latitude as number,
                longitude: place.longitude as number,
                kind: "place" as const,
            }))),
        ...(hasCurrentLocation ? [{
            id: "current",
            title: "현재 위치",
            latitude: currentLocation.latitude,
            longitude: currentLocation.longitude,
            kind: "current-location" as const,
        }] : []),
    ];

    const toggleDetail = async (contentId: string) => {
        if (details[contentId]) {
            setDetails((current) => {
                const next = { ...current };
                delete next[contentId];
                return next;
            });
            return;
        }
        setDetailLoadingId(contentId);
        try {
            const detail = await getPlacePetDetail(contentId);
            setDetails((current) => ({ ...current, [contentId]: detail }));
        } finally {
            setDetailLoadingId(null);
        }
    };

    return (
        <section className="nearby-section">
            <div className="nearby-header">
                <div className="nearby-title-area">
                    <span className="nearby-eyebrow">NEARBY</span>
                    <h2 className="nearby-title">내 주변 동반 장소</h2>
                    <p className="nearby-description">
                        위치 동의 후 한국관광공사 반려동물 동반여행 장소를 가까운 순으로 확인하세요.
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
                        center={currentLocation}
                        markers={markers}
                        level={7}
                        onCenterChange={handleMapCenterChange}
                    />
                    {mapMoved && (
                        <button
                            type="button"
                            className="nearby-search-area-button"
                            onClick={() => void searchAt(mapCenter)}
                            disabled={locationLoading}
                        >
                            이 지역 검색
                        </button>
                    )}
                </div>
                <div className="nearby-map-status">
                    <div className="nearby-status-left">
                        <span className={locationLoading ? "nearby-location-dot loading" : "nearby-location-dot"} />
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

            {places.length > 0 && (
                <>
                    <div className="nearby-place-filters" aria-label="장소 종류 필터">
                        <span className="nearby-radius-label">검색 범위 · {radiusPolicy.label}</span>
                        <button
                            type="button"
                            className={selectedType === "all" ? "active" : ""}
                            onClick={() => setSelectedType("all")}
                        >
                            전체 <span>{places.length}</span>
                        </button>
                        {availableTypes.map((contentTypeId) => (
                            <button
                                type="button"
                                key={contentTypeId}
                                className={selectedType === contentTypeId ? "active" : ""}
                                onClick={() => setSelectedType(contentTypeId)}
                            >
                                {CONTENT_TYPE_NAMES[contentTypeId] ?? "기타"}
                                <span>{places.filter((place) => place.contentTypeId === contentTypeId).length}</span>
                            </button>
                        ))}
                    </div>
                    <div className="nearby-place-grid">
                    {filteredPlaces.map((place) => (
                        <article className="nearby-place-card" key={place.contentId}>
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
                                <h3>{place.title}</h3>
                                <p>{place.address ?? "주소 정보가 제공되지 않습니다."}</p>
                                <small>한국관광공사 반려동물 동반여행 정보</small>
                                <button
                                    type="button"
                                    className="nearby-detail-button"
                                    onClick={() => void toggleDetail(place.contentId)}
                                    disabled={detailLoadingId === place.contentId}
                                >
                                    {detailLoadingId === place.contentId
                                        ? "불러오는 중…"
                                        : details[place.contentId] ? "상세정보 닫기" : "동반 상세정보"}
                                </button>
                                {details[place.contentId] && (
                                    <dl className="nearby-pet-detail">
                                        {details[place.contentId].allowedCapacity && <><dt>동반 가능 범위</dt><dd>{details[place.contentId].allowedCapacity}</dd></>}
                                        {details[place.contentId].requiredItems && <><dt>필요사항</dt><dd>{details[place.contentId].requiredItems}</dd></>}
                                        {details[place.contentId].additionalInformation && <><dt>이용 안내</dt><dd>{details[place.contentId].additionalInformation}</dd></>}
                                        {details[place.contentId].safetyNotice && <><dt>안전 유의사항</dt><dd>{details[place.contentId].safetyNotice}</dd></>}
                                    </dl>
                                )}
                            </div>
                        </article>
                    ))}
                    </div>
                </>
            )}
        </section>
    );
}
