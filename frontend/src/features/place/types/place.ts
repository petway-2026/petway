import type { WeatherRegion } from "../../weather/types/weather";
import type { MapMarker } from "./map";

export interface SelectedPlace extends MapMarker {
  contentId: string;
  contentTypeId: string;
  address: string;
  region: WeatherRegion;
  thumbnailUrl?: string;
}
