package com.petway.walk.dto;

import com.petway.compatibility.CompatibilityStatus;
import com.petway.place.dto.PlaceCompatibilityResponse.RequirementItem;
import com.petway.walk.policy.WalkFitGrade;
import com.petway.weather.domain.ForecastType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WalkFitRecommendationResponse(
        LocalDate targetDate,
        PetSummary pet,
        List<Recommendation> recommendations,
        String message
) {
    public WalkFitRecommendationResponse {
        recommendations = List.copyOf(recommendations);
    }

    public record PetSummary(
            Long petId,
            String petName
    ) {
    }

    public record Recommendation(
            int rank,
            String contentId,
            String title,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String imageUrl,
            WalkFit walkFit,
            Compatibility compatibility,
            Weather weather
    ) {
    }

    public record WalkFit(
            int score,
            WalkFitGrade grade,
            List<String> reasons
    ) {
        public WalkFit {
            reasons = List.copyOf(reasons);
        }
    }

    public record Compatibility(
            CompatibilityStatus status,
            List<RequirementItem> requirements,
            boolean needsConfirmation
    ) {
        public Compatibility {
            requirements = List.copyOf(requirements);
        }
    }

    public record Weather(
            boolean available,
            ForecastType forecastType,
            String summary,
            String temperature,
            String precipitation
    ) {
        public static Weather unavailable(ForecastType forecastType) {
            return new Weather(false, forecastType, null, null, null);
        }
    }
}
