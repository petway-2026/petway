/* 날짜별 장소·준비물 스냅샷의 저장, 목록·상세 조회, 삭제 API와 응답 타입을 제공한다. */
import { apiFetch } from "../../../shared/api/client";
import { API_URL } from "../../../shared/api/config";

export interface SavedTripPlace {
    contentId: string;
    contentTypeId: string;
    title: string;
    address: string | null;
    latitude: number | null;
    longitude: number | null;
    thumbnailUrl: string | null;
    preparations?: string[];
}

export interface SavedTripDay {
    date: string;
    places: SavedTripPlace[];
}

export interface SavedTrip {
    id: number;
    title: string;
    regionName: string;
    startDate: string;
    endDate: string;
    transportMode: "DRIVING" | "TRANSIT";
    placeCount: number;
    itinerary: { days: SavedTripDay[] };
    createdAt: string;
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
    const response = await apiFetch(`${API_URL}${url}`, { credentials: "include", ...options });
    if (!response.ok) {
        let message = "저장한 여행일정을 처리하지 못했습니다.";
        try { message = ((await response.json()) as { message?: string }).message ?? message; } catch { /* noop */ }
        throw new Error(message);
    }
    return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

export function saveTrip(input: Omit<SavedTrip, "id" | "placeCount" | "createdAt">): Promise<SavedTrip> {
    return request("/api/saved-trips", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
    });
}

export function getSavedTrips(): Promise<SavedTrip[]> { return request("/api/saved-trips"); }
export function getSavedTrip(id: number): Promise<SavedTrip> { return request(`/api/saved-trips/${id}`); }
export function deleteSavedTrip(id: number): Promise<void> { return request(`/api/saved-trips/${id}`, { method: "DELETE" }); }
