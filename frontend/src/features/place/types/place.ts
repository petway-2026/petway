/* 지도에서 선택한 장소의 표시 정보와 관광 콘텐츠 식별자를 묶는다. */
import type { WeatherRegion } from "../../weather/types/weather";
import type { MapMarker } from "./map";

export interface SelectedPlace extends MapMarker {
  contentId: string;
  contentTypeId: string;
  address: string;
  region: WeatherRegion;
  thumbnailUrl?: string;
}
