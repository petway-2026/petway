package com.petway.trip.dto;

import java.time.LocalDate;
import java.util.List;

public record SavedTripPayload(List<DayPlan> days) {
    public SavedTripPayload { days = days == null ? List.of() : List.copyOf(days); }

    public record DayPlan(LocalDate date, List<PlacePlan> places) {
        public DayPlan { places = places == null ? List.of() : List.copyOf(places); }
    }

    public record PlacePlan(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            Double latitude,
            Double longitude,
            String thumbnailUrl
    ) {}
}
