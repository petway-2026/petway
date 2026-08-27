import {
    useEffect,
    useRef,
    useState,
} from "react";

import {
    loadKakaoMap,
} from "../utils/loadKakaoMap";

import type {
    MapCenter,
    MapMarker,
} from "../types/map";

import "../styles/map.css";


interface KakaoMapProps {
    center: MapCenter;
    level?: number;
    markers?: MapMarker[];
}


export default function KakaoMap({
                                     center,
                                     level = 5,
                                     markers = [],
                                 }: KakaoMapProps) {

    const mapContainerRef =
        useRef<HTMLDivElement | null>(
            null
        );

    const mapRef =
        useRef<any>(null);

    const markerRefs =
        useRef<any[]>([]);


    const [
        error,
        setError,
    ] =
        useState<string | null>(null);


    /*
     * 지도 최초 생성
     */
    useEffect(() => {

        let mounted = true;


        const initializeMap =
            async () => {

                try {

                    await loadKakaoMap();


                    if (
                        !mounted ||
                        !mapContainerRef.current
                    ) {
                        return;
                    }


                    const kakao =
                        window.kakao;


                    const mapCenter =
                        new kakao.maps.LatLng(
                            center.latitude,
                            center.longitude
                        );


                    const map =
                        new kakao.maps.Map(
                            mapContainerRef.current,
                            {
                                center: mapCenter,
                                level,
                            }
                        );


                    mapRef.current =
                        map;


                    setError(null);

                } catch (error) {

                    console.error(
                        "카카오 지도 초기화 실패:",
                        error
                    );


                    if (mounted) {
                        setError(
                            "지도를 불러오지 못했습니다."
                        );
                    }

                }

            };


        initializeMap();


        return () => {
            mounted = false;
        };

    }, []);


    /*
     * center 변경
     *
     * geolocation 응답이 오면
     * 지도 중심을 현재 위치로 이동
     */
    useEffect(() => {

        if (
            !mapRef.current ||
            !window.kakao
        ) {
            return;
        }


        const position =
            new window.kakao.maps.LatLng(
                center.latitude,
                center.longitude
            );


        mapRef.current.setCenter(
            position
        );

    }, [
        center.latitude,
        center.longitude,
    ]);


    useEffect(() => {
        if (!mapRef.current) {
            return;
        }

        mapRef.current.setLevel(level);
    }, [level]);


    /*
     * marker 변경
     */
    useEffect(() => {

        if (
            !mapRef.current ||
            !window.kakao
        ) {
            return;
        }


        const kakao =
            window.kakao;


        /*
         * 기존 마커 삭제
         */
        markerRefs.current.forEach(
            (marker) => {
                marker.setMap(null);
            }
        );


        markerRefs.current = [];


        /*
         * 신규 마커 생성
         */
        const newMarkers =
            markers.map(
                (markerData) => {

                    const position =
                        new kakao.maps.LatLng(
                            markerData.latitude,
                            markerData.longitude
                        );


                    const marker =
                        new kakao.maps.Marker({
                            position,
                            map: mapRef.current,
                        });


                    /*
                     * 마커 클릭 시
                     * 장소명 표시
                     */
                    const infoWindow =
                        new kakao.maps.InfoWindow({
                            content: `
                <div
                  style="
                    padding:8px 12px;
                    font-size:12px;
                    white-space:nowrap;
                  "
                >
                  ${markerData.title}
                </div>
              `,
                        });


                    kakao.maps.event.addListener(
                        marker,
                        "click",
                        () => {

                            infoWindow.open(
                                mapRef.current,
                                marker
                            );

                        }
                    );


                    return marker;

                }
            );


        markerRefs.current =
            newMarkers;


        return () => {

            newMarkers.forEach(
                (marker) => {
                    marker.setMap(null);
                }
            );

        };

    }, [markers]);


    return (
        <div className="kakao-map-wrapper">

            {error && (
                <div className="kakao-map-error">
                    {error}
                </div>
            )}


            <div
                ref={mapContainerRef}
                className="kakao-map-container"
            />

        </div>
    );
}
