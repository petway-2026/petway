/* 방문 순서별 장소 좌표로 자동차·대중교통 경로를 요청하며 요청 취소 신호를 전달한다. */
import { apiFetch } from "../../../shared/api/client";
import { API_URL } from "../../../shared/api/config";

export interface DrivingRoute {
    totalDistanceMeters: number;
    totalDurationSeconds: number;
    legs: Array<{
        fromId: string;
        toId: string;
        distanceMeters: number;
        durationSeconds: number;
    }>;
    path: Array<{
        latitude: number;
        longitude: number;
    }>;
}

export interface TransitRoute {
    totalDurationMinutes: number;
    totalFare: number;
    legs: Array<{
        fromId: string;
        toId: string;
        durationMinutes: number;
        fare: number;
        transfers: number;
        steps: Array<{
            type: "WALK" | "BUS" | "SUBWAY";
            durationMinutes: number;
            distanceMeters: number;
            description: string;
        }>;
    }>;
}

export interface RouteStopInput {
    id: string;
    name: string;
    latitude: number;
    longitude: number;
}

export async function getDrivingRoute(
    stops: RouteStopInput[],
    signal?: AbortSignal,
): Promise<DrivingRoute> {
    const response = await apiFetch(`${API_URL}/api/routes/driving`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        signal,
        body: JSON.stringify({ stops }),
    });
    if (!response.ok) throw new Error("실제 도로 경로를 불러오지 못했습니다.");
    return response.json() as Promise<DrivingRoute>;
}

export async function getTransitRoute(stops: RouteStopInput[], signal?: AbortSignal): Promise<TransitRoute> {
    const response = await apiFetch(`${API_URL}/api/routes/transit`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        signal,
        body: JSON.stringify({ stops }),
    });
    if (!response.ok) throw new Error("대중교통 경로를 불러오지 못했습니다.");
    return response.json() as Promise<TransitRoute>;
}
