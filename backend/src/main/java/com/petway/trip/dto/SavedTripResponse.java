package com.petway.trip.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavedTripResponse(
        Long id,
        String title,
        String regionName,
        LocalDate startDate,
        LocalDate endDate,
        String transportMode,
        int placeCount,
        SavedTripPayload itinerary,
        LocalDateTime createdAt
) {}
