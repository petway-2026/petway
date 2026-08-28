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
