import {
    useEffect,
    useRef,
    useState,
} from "react";

import {
    loadKakaoMap,
} from "../utils/loadKakaoMap";

import currentLocationMarkerUrl
    from "../assets/current-location-marker.svg";

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

    const openInfoWindowRef =
        useRef<any>(null);

    const openMarkerRef =
        useRef<any>(null);

    const [
        mapReady,
        setMapReady,
    ] =
        useState(false);


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

                    setMapReady(true);


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
        mapReady,
        center.latitude,
        center.longitude,
    ]);


    useEffect(() => {
        if (!mapRef.current) {
            return;
        }

        mapRef.current.setLevel(level);
    }, [mapReady, level]);


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

        openInfoWindowRef.current?.close();
        openInfoWindowRef.current = null;
        openMarkerRef.current = null;


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


                    const markerOptions: Record<string, unknown> = {
                        position,
                        map: mapRef.current,
                    };

                    if (markerData.kind === "current-location") {
                        markerOptions.image =
                            new kakao.maps.MarkerImage(
                                currentLocationMarkerUrl,
                                new kakao.maps.Size(32, 32),
                                {
                                    offset:
                                        new kakao.maps.Point(16, 16),
                                }
                            );
                    }

                    const marker =
                        new kakao.maps.Marker(markerOptions);


                    /*
                     * 마커 클릭 시
                     * 장소명 표시
                     */
                    const infoContent =
                        document.createElement("div");
                    infoContent.textContent = markerData.title;
                    infoContent.style.padding = "8px 12px";
                    infoContent.style.fontSize = "12px";
                    infoContent.style.whiteSpace = "nowrap";

                    const infoWindow =
                        new kakao.maps.InfoWindow({
                            content: infoContent,
                        });


                    kakao.maps.event.addListener(
                        marker,
                        "click",
                        () => {
                            if (
                                openMarkerRef.current === marker
                            ) {
                                infoWindow.close();
                                openInfoWindowRef.current = null;
                                openMarkerRef.current = null;

                                return;
                            }

                            openInfoWindowRef.current?.close();

                            infoWindow.open(
                                mapRef.current,
                                marker
                            );

                            openInfoWindowRef.current = infoWindow;
                            openMarkerRef.current = marker;

                        }
                    );


                    return marker;

                }
            );


        markerRefs.current =
            newMarkers;


        return () => {

            openInfoWindowRef.current?.close();
            openInfoWindowRef.current = null;
            openMarkerRef.current = null;

            newMarkers.forEach(
                (marker) => {
                    marker.setMap(null);
                }
            );

        };

    }, [mapReady, markers]);


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
