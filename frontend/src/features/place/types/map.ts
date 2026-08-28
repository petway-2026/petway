export interface MapCenter {
    latitude: number;
    longitude: number;
}


export interface MapMarker {
    id: string | number;
    title: string;
    latitude: number;
    longitude: number;
    kind?: "place" | "current-location";
}
