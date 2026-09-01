import type { CompatibilityStatus } from "../../place/api/placeApi";
import type { ForecastType } from "../../weather/types/weather";

export type WalkFitGrade = "VERY_GOOD" | "GOOD" | "CAUTION" | "LOW";

export interface WalkFitRequirement {
  type: string;
  label: string;
  scope: "UNSPECIFIED" | "INDOOR" | "OUTDOOR";
}

export interface WalkFitRecommendationResponse {
  targetDate: string;
  pet: {
    petId: number;
    petName: string;
  };
  recommendations: WalkFitRecommendation[];
  message: string;
}

export interface WalkFitRecommendation {
  rank: number;
  contentId: string;
  title: string;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  imageUrl: string | null;
  walkFit: {
    score: number;
    grade: WalkFitGrade;
    reasons: string[];
  };
  compatibility: {
    status: CompatibilityStatus;
    requirements: WalkFitRequirement[];
    needsConfirmation: boolean;
  };
  weather: {
    available: boolean;
    forecastType: ForecastType;
    summary: string | null;
    temperature: string | null;
    precipitation: string | null;
  };
}
