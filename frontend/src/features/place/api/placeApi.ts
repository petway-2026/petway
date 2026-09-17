/* 장소 검색·동반 원문·반려견 판정 API를 제공한다. 여러 페이지 조회에는 후보 수와 요청 횟수 제한을 적용할 수 있다. */
import { apiFetch } from "../../../shared/api/client";
import { API_URL } from "../../../shared/api/config";

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
    telephone?: string | null;
    homepage?: string | null;
    operatingHours?: string | null;
    closedDays?: string | null;
    parking?: string | null;
    price?: string | null;
    indoor?: string | null;
    outdoor?: string | null;
    description?: string | null;
    source?: string | null;
}

export interface PlaceSearchResponse {
    page: number;
    size: number;
    totalCount: number;
    items: PlaceSummary[];
}

export interface NearbySearchLimits {
    targetUniqueCount?: number;
    maxPages?: number;
    /** Fetch every page reported by the API instead of using the default preview limits. */
    fetchAll?: boolean;
}

export interface PlacePetDetail {
    accompanimentType: string | null;
    allowedCapacity: string | null;
    requiredItems: string | null;
    additionalInformation: string | null;
    safetyNotice: string | null;
    availableFacilities: string | null;
    providedItems: string | null;
    purchasableItems: string | null;
    rentableItems: string | null;
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
        reasonGroups?: {
            unavailable: string[];
            confirmation: string[];
            conditions: string[];
            allowed: string[];
        } | null;
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
    signal?: AbortSignal,
): Promise<PlaceSearchResponse> {
    const query = new URLSearchParams({
        areaCode,
        page: String(page),
        size: String(size),
    });
    const response = await apiFetch(`${API_URL}/api/places?${query}`, { signal });

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

export async function searchAllPlaces(areaCode: string, signal?: AbortSignal): Promise<PlaceSearchResponse> {
    const pageSize = 100;
    const firstPage = await searchPlaces(areaCode, pageSize, 1, signal);
    signal?.throwIfAborted();
    const pageCount = Math.ceil(firstPage.totalCount / pageSize);
    const items = [...firstPage.items];

    for (let page = 2; page <= pageCount; page += 1) {
        const response = await searchPlaces(areaCode, pageSize, page, signal);
        signal?.throwIfAborted();
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
    size = 100,
    signal?: AbortSignal
): Promise<PlaceSearchResponse> {
    const query = new URLSearchParams({
        latitude: String(latitude),
        longitude: String(longitude),
        radius: String(radius),
        page: String(page),
        size: String(size),
    });
    const response = await apiFetch(`${API_URL}/api/places/nearby?${query}`, { signal });

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
    radius = 20000,
    onProgress?: (response: PlaceSearchResponse) => void,
    limits: NearbySearchLimits = {},
    signal?: AbortSignal
): Promise<PlaceSearchResponse> {
    const pageSize = 100;
    const targetUniqueCount = limits.fetchAll
        ? undefined
        : Math.max(1, limits.targetUniqueCount ?? 200);
    const firstPage = await searchNearbyPlaces(
        latitude,
        longitude,
        radius,
        1,
        pageSize,
        signal
    );
    signal?.throwIfAborted();
    const defaultMaxPages = limits.fetchAll
        ? Math.ceil(firstPage.totalCount / pageSize)
        : 5;
    const maxPages = Math.max(1, limits.maxPages ?? defaultMaxPages);
    const pageCount = Math.min(Math.ceil(firstPage.totalCount / pageSize), maxPages);
    const items = [...firstPage.items];
    const uniqueItems = () => Array.from(
        new Map(items.map((place) => [place.contentId, place])).values()
    );
    const progress = () => {
        const current = uniqueItems();
        onProgress?.({ page: 1, size: current.length, totalCount: firstPage.totalCount, items: current });
    };
    progress();

    for (let page = 2; page <= pageCount
        && (targetUniqueCount === undefined || uniqueItems().length < targetUniqueCount); page += 1) {
        const response = await searchNearbyPlaces(latitude, longitude, radius, page, pageSize, signal);
        signal?.throwIfAborted();
        items.push(...response.items);
        progress();
    }

    const resultItems = uniqueItems();

    return {
        page: 1,
        size: resultItems.length,
        totalCount: firstPage.totalCount,
        items: resultItems,
    };
}

export async function getPlacePetDetail(
    contentId: string,
    signal?: AbortSignal
): Promise<PlacePetDetail> {
    const response = await apiFetch(`${API_URL}/api/places/${contentId}/pet-detail`, {
        signal,
    });
    if (!response.ok) throw new Error("반려동물 동반 상세정보를 불러오지 못했습니다.");
    return response.json() as Promise<PlacePetDetail>;
}

export async function getPlaceCompatibility(
    contentId: string,
    signal?: AbortSignal
): Promise<PlaceCompatibility> {
    const response = await apiFetch(
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

export type PlaceCompatibilityEntry =
    | { result: PlaceCompatibility; error?: never }
    | { result?: never; error: string };

// 가까운 장소부터 최대 4개씩 조회한다. 취소된 검색의 응답은 화면에 전달하지 않는다.
export async function loadPlaceCompatibilities(
    contentIds: string[],
    signal: AbortSignal,
    onResult: (contentId: string, entry: PlaceCompatibilityEntry) => void,
): Promise<void> {
    const pending = [...new Set(contentIds)];
    let next = 0;
    let sharedError: string | undefined;
    await Promise.all(Array.from({ length: Math.min(4, pending.length) }, async () => {
        while (!signal.aborted && next < pending.length) {
            const contentId = pending[next++];
            let entry: PlaceCompatibilityEntry;
            if (sharedError) {
                entry = { error: sharedError };
            } else {
                try {
                    entry = { result: await getPlaceCompatibility(contentId, signal) };
                } catch (cause) {
                    if (signal.aborted) return;
                    const message = cause instanceof Error ? cause.message : "동반 판정을 불러오지 못했습니다.";
                    entry = { error: message };
                    if (cause instanceof PlaceCompatibilityApiError
                        && ([401, 403].includes(cause.status) || message.includes("대표 반려견"))) {
                        sharedError = message;
                    }
                }
            }
            if (!signal.aborted) onResult(contentId, entry);
        }
    }));
}
