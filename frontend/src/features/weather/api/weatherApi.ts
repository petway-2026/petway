import { apiFetch } from "../../../shared/api/client";
import type {
  WeatherForecastRequest,
  WeatherForecastResponse,
} from "../types/weather";

import { API_URL } from "../../../shared/api/config";

export class WeatherApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "WeatherApiError";
    this.status = status;
  }
}

async function parseError(response: Response): Promise<string> {
  try {
    const data: unknown = await response.json();

    if (
      typeof data === "object" &&
      data !== null &&
      "message" in data &&
      typeof data.message === "string"
    ) {
      return data.message;
    }
  } catch {
    // JSON이 아닌 오류 응답은 공통 메시지로 처리합니다.
  }

  return "날씨 정보를 가져오지 못했습니다.";
}

export async function getWeather(
  request: WeatherForecastRequest,
  signal?: AbortSignal,
): Promise<WeatherForecastResponse> {
  const params = new URLSearchParams({
    latitude: String(request.latitude),
    longitude: String(request.longitude),
    targetDate: request.targetDate,
    region: request.region,
  });

  const response = await apiFetch(`${API_URL}/api/weather?${params.toString()}`, {
    method: "GET",
    credentials: "include",
    signal,
  });

  if (!response.ok) {
    throw new WeatherApiError(response.status, await parseError(response));
  }

  return response.json();
}
