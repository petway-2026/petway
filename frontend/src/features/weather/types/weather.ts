/* 단기·중기·계절 전망의 공통 응답 및 예보별 선택 필드를 정의한다. */
export type WeatherRegion =
  | "SEOUL_INCHEON_GYEONGGI"
  | "GANGWON_YEONGSEO"
  | "GANGWON_YEONGDONG"
  | "DAEJEON_SEJONG_CHUNGNAM"
  | "CHUNGBUK"
  | "GWANGJU_JEONNAM"
  | "JEONBUK"
  | "DAEGU_GYEONGBUK"
  | "BUSAN_ULSAN_GYEONGNAM"
  | "JEJU";

export type ForecastType = "SHORT_TERM" | "MID_TERM" | "SEASONAL_OUTLOOK";

export type WeatherCondition =
  | "CLEAR"
  | "PARTLY_CLOUDY"
  | "CLOUDY"
  | "RAIN"
  | "RAIN_OR_SNOW"
  | "SNOW"
  | "SHOWER"
  | "UNKNOWN";

export type OutlookLevel =
  | "BELOW_NORMAL"
  | "NEAR_NORMAL"
  | "ABOVE_NORMAL"
  | "MIXED";

export interface OutlookProbabilities {
  belowNormal: number;
  nearNormal: number;
  aboveNormal: number;
}

export interface WeatherDisplay {
  temperature: string;
  precipitation: string;
  weather: string;
  summary: string;
}

export interface WeatherForecastRequest {
  latitude: number;
  longitude: number;
  targetDate: string;
  region: WeatherRegion;
}

export interface WeatherForecastResponse {
  targetDate: string;
  forecastType: ForecastType;
  regionName?: string;
  minimumTemperature?: number;
  maximumTemperature?: number;
  rainProbability?: number;
  weatherCondition?: WeatherCondition;
  display: WeatherDisplay;
  temperatureOutlook?: OutlookLevel;
  precipitationOutlook?: OutlookLevel;
  temperatureProbabilities?: OutlookProbabilities;
  precipitationProbabilities?: OutlookProbabilities;
  announcedAt?: string;
  notice?: string;
  source: string;
}
