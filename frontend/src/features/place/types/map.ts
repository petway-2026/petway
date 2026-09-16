/* 지도 중심·경계·마커의 공통 좌표 타입을 정의한다. */
export interface MapCenter {
    latitude: number;
    longitude: number;
}

export interface MapBounds {
    south: number;
    west: number;
    north: number;
    east: number;
}


export interface MapMarker {
    id: string | number;
    title: string;
    latitude: number;
    longitude: number;
    kind?: "place" | "hospital" | "pharmacy" | "current-location";
    label?: string;
}
