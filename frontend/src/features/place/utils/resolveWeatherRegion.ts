/* 장소의 관광 지역·시군구 코드로 날씨 API에 전달할 예보 권역을 선택한다. */
import type { WeatherRegion } from "../../weather/types/weather";

const GANGWON_YEONGDONG_SIGUNGU_CODES = new Set([
  "1", // 강릉시
  "2", // 고성군
  "3", // 동해시
  "4", // 삼척시
  "5", // 속초시
  "7", // 양양군
  "14", // 태백시
]);

export function resolveWeatherRegion(
  areaCode: string | null,
  sigunguCode: string | null,
): WeatherRegion | null {
  switch (areaCode) {
    case "1":
    case "2":
    case "31":
      return "SEOUL_INCHEON_GYEONGGI";
    case "32":
      return sigunguCode && GANGWON_YEONGDONG_SIGUNGU_CODES.has(sigunguCode)
        ? "GANGWON_YEONGDONG"
        : "GANGWON_YEONGSEO";
    case "3":
    case "8":
    case "34":
      return "DAEJEON_SEJONG_CHUNGNAM";
    case "33":
      return "CHUNGBUK";
    case "5":
    case "38":
      return "GWANGJU_JEONNAM";
    case "37":
      return "JEONBUK";
    case "4":
    case "35":
      return "DAEGU_GYEONGBUK";
    case "6":
    case "7":
    case "36":
      return "BUSAN_ULSAN_GYEONGNAM";
    case "39":
      return "JEJU";
    default:
      return null;
  }
}
