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
    MapBounds,
    MapMarker,
} from "../types/map";

import "../styles/map.css";


interface KakaoMapProps {
    center: MapCenter;
    level?: number;
    markers?: MapMarker[];
    routePath?: MapCenter[];
    selectedMarkerId?: MapMarker["id"] | null;
    onMarkerSelect?: (marker: MapMarker | null) => void;
    onCenterChange?: (center: MapCenter) => void;
    onBoundsChange?: (bounds: MapBounds) => void;
}


export default function KakaoMap({
                                     center,
                                     level = 5,
                                     markers = [],
                                     routePath = [],
                                     selectedMarkerId = null,
                                     onMarkerSelect,
                                     onCenterChange,
                                     onBoundsChange,
                                 }: KakaoMapProps) {

    const mapContainerRef =
        useRef<HTMLDivElement | null>(
            null
        );

    const mapRef =
        useRef<any>(null);

    const markerRefs =
        useRef<any[]>([]);

    const routeLineRef =
        useRef<any>(null);

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

        if (selectedMarkerId !== null) {
            return;
        }

        openInfoWindowRef.current?.close();
        openInfoWindowRef.current = null;
        openMarkerRef.current = null;

    }, [selectedMarkerId]);


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

    useEffect(() => {
        if (!mapRef.current || !window.kakao || (!onCenterChange && !onBoundsChange)) return;

        const map = mapRef.current;
        const handleIdle = () => {
            const nextCenter = map.getCenter();
            onCenterChange?.({
                latitude: nextCenter.getLat(),
                longitude: nextCenter.getLng(),
            });
            if (onBoundsChange) {
                const bounds = map.getBounds();
                const southWest = bounds.getSouthWest();
                const northEast = bounds.getNorthEast();
                onBoundsChange({
                    south: southWest.getLat(),
                    west: southWest.getLng(),
                    north: northEast.getLat(),
                    east: northEast.getLng(),
                });
            }
        };

        window.kakao.maps.event.addListener(map, "idle", handleIdle);
        return () => {
            window.kakao.maps.event.removeListener(map, "idle", handleIdle);
        };
    }, [mapReady, onBoundsChange, onCenterChange]);


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
                    } else if (markerData.label) {
                        const escapedLabel = markerData.label.replace(/[^0-9]/g, "").slice(0, 2);
                        const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="40" height="48" viewBox="0 0 40 48"><path fill="#526f5d" stroke="#fff" stroke-width="3" d="M20 1.5c-10.2 0-18.5 8.1-18.5 18.2C1.5 32.8 20 46.5 20 46.5s18.5-13.7 18.5-26.8C38.5 9.6 30.2 1.5 20 1.5Z"/><circle cx="20" cy="20" r="12" fill="#fff"/><text x="20" y="25" text-anchor="middle" font-family="Arial,sans-serif" font-size="15" font-weight="700" fill="#3f5849">${escapedLabel}</text></svg>`;
                        markerOptions.image = new kakao.maps.MarkerImage(
                            `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`,
                            new kakao.maps.Size(40, 48),
                            { offset: new kakao.maps.Point(20, 48) }
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
                                onMarkerSelect?.(null);

                                return;
                            }

                            openInfoWindowRef.current?.close();

                            infoWindow.open(
                                mapRef.current,
                                marker
                            );

                            openInfoWindowRef.current = infoWindow;
                            openMarkerRef.current = marker;
                            onMarkerSelect?.(markerData);

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

    }, [mapReady, markers, onMarkerSelect]);

    useEffect(() => {
        if (!mapRef.current || !window.kakao) return;

        routeLineRef.current?.setMap(null);
        routeLineRef.current = null;
        if (routePath.length < 2) return;

        const kakao = window.kakao;
        const path = routePath.map((point) => new kakao.maps.LatLng(point.latitude, point.longitude));
        const routeLine = new kakao.maps.Polyline({
            map: mapRef.current,
            path,
            strokeWeight: 5,
            strokeColor: "#526f5d",
            strokeOpacity: 0.82,
            strokeStyle: "solid",
        });
        routeLineRef.current = routeLine;

        const bounds = new kakao.maps.LatLngBounds();
        path.forEach((position) => bounds.extend(position));
        mapRef.current.setBounds(bounds, 52, 52, 52, 52);

        return () => {
            routeLine.setMap(null);
            if (routeLineRef.current === routeLine) routeLineRef.current = null;
        };
    }, [mapReady, routePath]);


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
