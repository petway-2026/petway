const API_URL =
    import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export interface PlaceSummary {
    contentId: string;
    contentTypeId: string;
    areaCode: string | null;
    sigunguCode: string | null;
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

export type CompatibilityStatus =
    | "AVAILABLE"
    | "CONDITIONAL"
    | "NEED_CONFIRMATION"
    | "UNAVAILABLE";

export interface CompatibilityRequirement {
    type: string;
    label: string;
    scope: "UNSPECIFIED" | "INDOOR" | "OUTDOOR";
}

export interface PlaceCompatibility {
    contentId: string;
    petId: number;
    petName: string;
    officialInformation: PlacePetDetail;
    petwayAssessment: {
        status: CompatibilityStatus;
        reasons: string[];
        requirements: CompatibilityRequirement[];
        needsConfirmation: boolean;
    };
}

export class PlaceCompatibilityApiError extends Error {
    readonly status: number;

    constructor(status: number, message: string) {
        super(message);
        this.name = "PlaceCompatibilityApiError";
        this.status = status;
    }
}

export async function searchPlaces(
    areaCode: string,
    size = 100,
    page = 1,
): Promise<PlaceSearchResponse> {
    const query = new URLSearchParams({
        areaCode,
        page: String(page),
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

export async function searchAllPlaces(areaCode: string): Promise<PlaceSearchResponse> {
    const pageSize = 100;
    const firstPage = await searchPlaces(areaCode, pageSize, 1);
    const pageCount = Math.min(Math.ceil(firstPage.totalCount / pageSize), 50);
    const items = [...firstPage.items];

    for (let page = 2; page <= pageCount; page += 1) {
        const response = await searchPlaces(areaCode, pageSize, page);
        items.push(...response.items);
    }

    const uniqueItems = Array.from(
        new Map(items.map((place) => [place.contentId, place])).values()
    );

    return {
        page: 1,
        size: uniqueItems.length,
        totalCount: firstPage.totalCount,
        items: uniqueItems,
    };
}

export async function searchNearbyPlaces(
    latitude: number,
    longitude: number,
    radius = 20000,
    page = 1,
    size = 100
): Promise<PlaceSearchResponse> {
    const query = new URLSearchParams({
        latitude: String(latitude),
        longitude: String(longitude),
        radius: String(radius),
        page: String(page),
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

export async function searchAllNearbyPlaces(
    latitude: number,
    longitude: number,
    radius = 20000
): Promise<PlaceSearchResponse> {
    const pageSize = 100;
    const firstPage = await searchNearbyPlaces(
        latitude,
        longitude,
        radius,
        1,
        pageSize
    );
    const pageCount = Math.min(Math.ceil(firstPage.totalCount / pageSize), 50);
    const items = [...firstPage.items];

    for (let page = 2; page <= pageCount; page += 1) {
        const response = await searchNearbyPlaces(
            latitude,
            longitude,
            radius,
            page,
            pageSize
        );
        items.push(...response.items);
    }

    const uniqueItems = Array.from(
        new Map(items.map((place) => [place.contentId, place])).values()
    );

    return {
        page: 1,
        size: uniqueItems.length,
        totalCount: firstPage.totalCount,
        items: uniqueItems,
    };
}

export async function getPlacePetDetail(
    contentId: string,
    signal?: AbortSignal
): Promise<PlacePetDetail> {
    const response = await fetch(`${API_URL}/api/places/${contentId}/pet-detail`, {
        signal,
    });
    if (!response.ok) throw new Error("반려동물 동반 상세정보를 불러오지 못했습니다.");
    return response.json() as Promise<PlacePetDetail>;
}

export async function getPlaceCompatibility(
    contentId: string,
    signal?: AbortSignal
): Promise<PlaceCompatibility> {
    const response = await fetch(
        `${API_URL}/api/places/${contentId}/compatibility`,
        {
            credentials: "include",
            signal,
        }
    );

    if (!response.ok) {
        let message = "내 반려견의 동반 가능 여부를 확인하지 못했습니다.";
        try {
            const error = await response.json() as { message?: string };
            message = error.message ?? message;
        } catch {
            // JSON 오류 응답이 아니면 사용자용 기본 문구를 사용합니다.
        }
        throw new PlaceCompatibilityApiError(response.status, message);
    }

    return response.json() as Promise<PlaceCompatibility>;
}
