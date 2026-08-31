const API_URL =
    import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export interface PlaceSummary {
    contentId: string;
    contentTypeId: string;
    title: string;
    address: string | null;
    latitude: number | null;
    longitude: number | null;
    thumbnailUrl: string | null;
}

export interface PlaceSearchResponse {
    page: number;
    size: number;
    totalCount: number;
    items: PlaceSummary[];
}

export interface PlacePetDetail {
    accompanimentType: string | null;
    allowedCapacity: string | null;
    requiredItems: string | null;
    additionalInformation: string | null;
    safetyNotice: string | null;
}

export async function searchPlaces(
    areaCode: string,
    size = 100
): Promise<PlaceSearchResponse> {
    const query = new URLSearchParams({
        areaCode,
        page: "1",
        size: String(size),
    });
    const response = await fetch(`${API_URL}/api/places?${query}`);

    if (!response.ok) {
        let message = "반려동물 동반 장소를 불러오지 못했습니다.";

        try {
            const error = await response.json() as { message?: string };
            message = error.message ?? message;
        } catch {
            // JSON 오류 응답이 아니면 기본 안내를 사용합니다.
        }

        throw new Error(message);
    }

    return response.json() as Promise<PlaceSearchResponse>;
}

export async function searchNearbyPlaces(
    latitude: number,
    longitude: number,
    radius = 20000,
    size = 12
): Promise<PlaceSearchResponse> {
    const query = new URLSearchParams({
        latitude: String(latitude),
        longitude: String(longitude),
        radius: String(radius),
        size: String(size),
    });
    const response = await fetch(`${API_URL}/api/places/nearby?${query}`);

    if (!response.ok) {
        let message = "주변 반려동물 동반 장소를 불러오지 못했습니다.";
        try {
            const error = await response.json() as { message?: string };
            message = error.message ?? message;
        } catch {
            // 기본 안내 문구를 사용합니다.
        }
        throw new Error(message);
    }

    return response.json() as Promise<PlaceSearchResponse>;
}

export async function getPlacePetDetail(contentId: string): Promise<PlacePetDetail> {
    const response = await fetch(`${API_URL}/api/places/${contentId}/pet-detail`);
    if (!response.ok) throw new Error("반려동물 동반 상세정보를 불러오지 못했습니다.");
    return response.json() as Promise<PlacePetDetail>;
}
