import { apiFetch } from "../../../shared/api/client";
import type { WalkFitRecommendationResponse } from "../types/walkFit";

import { API_URL } from "../../../shared/api/config";

export class WalkFitApiError extends Error {
  readonly status: number;
  readonly code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = "WalkFitApiError";
    this.status = status;
    this.code = code;
  }
}

export async function getWalkFitRecommendations(
  latitude: number,
  longitude: number,
  targetDate: string,
  signal?: AbortSignal,
): Promise<WalkFitRecommendationResponse> {
  const query = new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    targetDate,
  });
  const response = await apiFetch(`${API_URL}/api/walk/recommendations?${query}`, {
    credentials: "include",
    signal,
  });

  if (!response.ok) {
    let message = "산책 추천을 불러오지 못했습니다.";
    let code: string | undefined;
    try {
      const error = (await response.json()) as { message?: string; code?: string };
      message = error.message ?? message;
      code = error.code;
    } catch {
      // JSON 오류 응답이 아니면 기본 사용자 안내를 사용합니다.
    }
    throw new WalkFitApiError(response.status, message, code);
  }

  return response.json() as Promise<WalkFitRecommendationResponse>;
}
