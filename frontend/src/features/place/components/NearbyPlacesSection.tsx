import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import KakaoMap from "./KakaoMap";

import type {
    MapCenter,
    MapMarker,
} from "../types/map";

import "../styles/nearby.css";


const DEFAULT_LOCATION: MapCenter = {
    latitude: 37.5665,
    longitude: 126.9780,
};


export default function NearbyPlacesSection() {
    const navigate = useNavigate();

    const [currentLocation, setCurrentLocation] =
        useState<MapCenter>(DEFAULT_LOCATION);

    const [markers, setMarkers] =
        useState<MapMarker[]>([]);

    const [locationLoading, setLocationLoading] =
        useState(true);

    const [locationMessage, setLocationMessage] =
        useState("현재 위치를 확인하고 있어요.");


    useEffect(() => {
        getCurrentLocation();
    }, []);


    const getCurrentLocation = () => {
        if (!navigator.geolocation) {
            setCurrentLocation(DEFAULT_LOCATION);

            setMarkers([
                {
                    id: "current",
                    title: "기본 위치",
                    latitude: DEFAULT_LOCATION.latitude,
                    longitude: DEFAULT_LOCATION.longitude,
                },
            ]);

            setLocationMessage(
                "현재 위치 기능을 사용할 수 없어 기본 위치를 표시합니다."
            );

            setLocationLoading(false);

            return;
        }


        setLocationLoading(true);

        setLocationMessage(
            "현재 위치를 확인하고 있어요."
        );


        navigator.geolocation.getCurrentPosition(
            (position) => {
                const location: MapCenter = {
                    latitude:
                    position.coords.latitude,

                    longitude:
                    position.coords.longitude,
                };


                setCurrentLocation(location);


                /*
                 * 현재는 TourAPI 연결 전이므로
                 * 현재 위치 마커만 표시
                 *
                 * 이후 TourAPI 주변 장소 응답을
                 * markers 배열에 추가하면 됨.
                 */
                setMarkers([
                    {
                        id: "current",
                        title: "내 위치",
                        latitude:
                        location.latitude,
                        longitude:
                        location.longitude,
                    },
                ]);


                setLocationMessage(
                    "현재 위치를 기준으로 주변 장소를 확인할 수 있어요."
                );


                setLocationLoading(false);
            },

            (error) => {
                console.error(
                    "현재 위치 조회 실패:",
                    error
                );


                setCurrentLocation(
                    DEFAULT_LOCATION
                );


                setMarkers([
                    {
                        id: "default",
                        title: "기본 위치",
                        latitude:
                        DEFAULT_LOCATION.latitude,

                        longitude:
                        DEFAULT_LOCATION.longitude,
                    },
                ]);


                if (
                    error.code ===
                    error.PERMISSION_DENIED
                ) {
                    setLocationMessage(
                        "위치 권한을 허용하면 내 주변 장소를 바로 확인할 수 있어요."
                    );
                } else {
                    setLocationMessage(
                        "현재 위치를 가져오지 못해 기본 위치를 표시합니다."
                    );
                }


                setLocationLoading(false);
            },

            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 60000,
            }
        );
    };


    const handleOpenFullMap = () => {
        navigate("/places");
    };


    return (
        <section className="nearby-section">

            <div className="nearby-header">

                <div className="nearby-title-area">

          <span className="nearby-eyebrow">
            NEARBY
          </span>

                    <h2 className="nearby-title">
                        내 주변 장소
                    </h2>

                    <p className="nearby-description">
                        현재 위치 주변의 반려동물 동반 장소를 찾아요.
                    </p>

                </div>


                <button
                    type="button"
                    className="nearby-map-button"
                    onClick={handleOpenFullMap}
                >
                    전체 지도 보기
                    <span aria-hidden="true">
            →
          </span>
                </button>

            </div>


            <div className="nearby-map-card">

                <div className="nearby-map">

                    <KakaoMap
                        center={currentLocation}
                        markers={markers}
                    />

                </div>


                <div className="nearby-map-status">

                    <div className="nearby-status-left">

            <span
                className={
                    locationLoading
                        ? "nearby-location-dot loading"
                        : "nearby-location-dot"
                }
            />

                        <span>
              {locationMessage}
            </span>

                    </div>


                    {!locationLoading && (
                        <button
                            type="button"
                            className="nearby-location-refresh"
                            onClick={getCurrentLocation}
                        >
                            위치 다시 찾기
                        </button>
                    )}

                </div>

            </div>

        </section>
    );
}