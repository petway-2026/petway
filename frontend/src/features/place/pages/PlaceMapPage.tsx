import { useEffect, useMemo, useState } from "react";

import { searchPlaces } from "../api/placeApi";
import KakaoMap from "../components/KakaoMap";
import type { MapMarker } from "../types/map";

import "../styles/map.css";

const SEOUL_AREA_CODE = "1";

export default function PlaceMapPage() {
    const [markers, setMarkers] = useState<MapMarker[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let active = true;

        searchPlaces(SEOUL_AREA_CODE)
            .then((result) => {
                if (!active) return;

                setMarkers(
                    result.items
                        .filter(
                            (place) =>
                                place.latitude !== null &&
                                place.longitude !== null
                        )
                        .map((place) => ({
                            id: place.contentId,
                            title: place.title,
                            latitude: place.latitude as number,
                            longitude: place.longitude as number,
                            kind: "place",
                        }))
                );
                setError(null);
            })
            .catch((cause: unknown) => {
                if (!active) return;
                setError(
                    cause instanceof Error
                        ? cause.message
                        : "반려동물 동반 장소를 불러오지 못했습니다."
                );
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
    }, []);

    const statusText = useMemo(() => {
        if (loading) return "관광공사 반려동물 동반 장소를 불러오는 중입니다.";
        if (error) return error;
        return `서울 지역 반려동물 동반 장소 ${markers.length}곳을 표시했습니다.`;
    }, [error, loading, markers.length]);

    return (
        <main className="place-map-page">
            <div className="place-map-container">
                <section className="place-map-heading">
                    <span className="place-map-eyebrow">EXPLORE PETWAY</span>
                    <h1>우리 아이와<br />어디로 갈까요?</h1>
                    <p>
                        한국관광공사 반려동물 동반여행 정보를 지도에서 확인해 보세요.
                    </p>
                </section>

                <section className="place-map-content">
                    <div className="place-map-toolbar">
                        <div className="place-map-toolbar-left">
                            <strong>서울 반려동물 동반 장소</strong>
                            <span>{statusText}</span>
                        </div>
                    </div>

                    <KakaoMap
                        center={{ latitude: 37.5665, longitude: 126.978 }}
                        level={8}
                        markers={markers}
                    />
                </section>
            </div>
        </main>
    );
}
