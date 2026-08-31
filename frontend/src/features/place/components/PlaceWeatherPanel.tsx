import { useEffect, useMemo, useState } from "react";

import { getWeather, WeatherApiError } from "../../weather/api/weatherApi";
import type {
  ForecastType,
  WeatherCondition,
  WeatherForecastResponse,
} from "../../weather/types/weather";
import type { SelectedPlace } from "../types/place";

interface PlaceWeatherPanelProps {
  place: SelectedPlace;
  onClose: () => void;
}

interface WeatherErrorView {
  title: string;
  message: string;
  retryable: boolean;
}

const FORECAST_LABELS: Record<ForecastType, string> = {
  SHORT_TERM: "단기예보",
  MID_TERM: "중기예보",
  SEASONAL_OUTLOOK: "3개월 전망",
};

const WEATHER_ICONS: Record<WeatherCondition, string> = {
  CLEAR: "☀️",
  PARTLY_CLOUDY: "⛅",
  CLOUDY: "☁️",
  RAIN: "🌧️",
  RAIN_OR_SNOW: "🌨️",
  SNOW: "❄️",
  SHOWER: "🌦️",
  UNKNOWN: "🌡️",
};

function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatAnnouncedAt(value?: string): string | null {
  if (!value) {
    return null;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function toWeatherErrorView(cause: unknown): WeatherErrorView {
  if (cause instanceof WeatherApiError) {
    if (cause.status === 400) {
      return {
        title: "날짜 또는 위치를 확인해 주세요.",
        message: cause.message,
        retryable: false,
      };
    }

    if (cause.status === 404) {
      return {
        title: "아직 제공된 예보가 없어요.",
        message: cause.message,
        retryable: false,
      };
    }

    if (cause.status === 504) {
      return {
        title: "날씨 조회 시간이 초과됐어요.",
        message: cause.message,
        retryable: true,
      };
    }

    if (cause.status >= 500) {
      return {
        title: "기상청 정보를 불러오지 못했어요.",
        message: cause.message,
        retryable: true,
      };
    }

    return {
      title: "날씨를 불러오지 못했어요.",
      message: cause.message,
      retryable: false,
    };
  }

  return {
    title: "날씨를 불러오지 못했어요.",
    message: "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
    retryable: true,
  };
}

export default function PlaceWeatherPanel({
  place,
  onClose,
}: PlaceWeatherPanelProps) {
  const [targetDate, setTargetDate] = useState("");
  const [forecast, setForecast] = useState<WeatherForecastResponse | null>(
    null,
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<WeatherErrorView | null>(null);
  const [retryKey, setRetryKey] = useState(0);

  const dateRange = useMemo(() => {
    const today = new Date();
    const maximum = new Date(today);
    maximum.setDate(maximum.getDate() + 90);

    return {
      minimum: formatLocalDate(today),
      maximum: formatLocalDate(maximum),
    };
  }, []);

  useEffect(() => {
    setForecast(null);
    setError(null);

    if (!targetDate) {
      setLoading(false);
      return;
    }

    const controller = new AbortController();
    setLoading(true);

    getWeather(
      {
        latitude: place.latitude,
        longitude: place.longitude,
        targetDate,
        region: place.region,
      },
      controller.signal,
      )
      .then(setForecast)
      .catch((requestError: unknown) => {
        if (controller.signal.aborted) {
          return;
        }

        setError(toWeatherErrorView(requestError));
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [
    place.contentId,
    place.latitude,
    place.longitude,
    place.region,
    retryKey,
    targetDate,
  ]);

  const announcedAt = formatAnnouncedAt(forecast?.announcedAt);
  const weatherIcon = forecast?.weatherCondition
    ? WEATHER_ICONS[forecast.weatherCondition]
    : "🌡️";

  return (
    <aside
      className="place-detail-panel"
      aria-label={`${place.title} 여행일 날씨`}
    >
      <section className="place-weather-section">
        <div className="place-weather-panel-header">
          <div className="place-weather-title-group">
            <span aria-hidden="true">📅</span>
            <div>
              <h3>여행일 날씨</h3>
              <p>방문할 날짜를 선택해 주세요.</p>
            </div>
          </div>
          <button
            type="button"
            className="place-detail-close"
            onClick={onClose}
            aria-label="여행일 날씨 닫기"
          >
            ×
          </button>
        </div>

        <label
          className="place-weather-date-label"
          htmlFor="place-weather-date"
        >
          여행일
        </label>
        <input
          id="place-weather-date"
          className="place-weather-date-input"
          type="date"
          min={dateRange.minimum}
          max={dateRange.maximum}
          value={targetDate}
          onChange={(event) => setTargetDate(event.target.value)}
        />

        <div className="place-weather-result" aria-live="polite">
          {!targetDate && (
            <div className="place-weather-empty">
              <span aria-hidden="true">🌤️</span>
              <p>여행일을 선택하면 해당 장소의 날씨를 확인할 수 있어요.</p>
            </div>
          )}

          {loading && (
            <div className="place-weather-loading">
              <span className="place-weather-spinner" aria-hidden="true" />
              날씨 정보를 확인하고 있어요.
            </div>
          )}

          {error && !loading && (
            <div className="place-weather-error" role="alert">
              <strong>{error.title}</strong>
              <span>{error.message}</span>
              {error.retryable && (
                <button
                  type="button"
                  onClick={() => setRetryKey((current) => current + 1)}
                >
                  다시 시도
                </button>
              )}
            </div>
          )}

          {forecast && !loading && !error && (
            <article className="place-weather-card">
              <div className="place-weather-card-top">
                <span className="place-weather-icon" aria-hidden="true">
                  {weatherIcon}
                </span>
                <div>
                  <span className="place-weather-forecast-type">
                    {FORECAST_LABELS[forecast.forecastType]}
                  </span>
                  <strong>{forecast.display.weather}</strong>
                </div>
              </div>

              <dl className="place-weather-metrics">
                <div>
                  <dt>기온</dt>
                  <dd>{forecast.display.temperature}</dd>
                </div>
                <div>
                  <dt>강수</dt>
                  <dd>{forecast.display.precipitation}</dd>
                </div>
              </dl>

              <p className="place-weather-summary">
                {forecast.display.summary}
              </p>

              {forecast.forecastType === "SEASONAL_OUTLOOK" && (
                <p className="place-weather-seasonal-notice">
                  정확한 일별 예보가 아닌 월별 기후 전망입니다.
                </p>
              )}

              <div className="place-weather-meta">
                {forecast.notice && <span>{forecast.notice}</span>}
                {announcedAt && <span>{announcedAt} 발표</span>}
                <span>출처: {forecast.source}</span>
              </div>
            </article>
          )}
        </div>
      </section>
    </aside>
  );
}
