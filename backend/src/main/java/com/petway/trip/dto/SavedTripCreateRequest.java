package com.petway.trip.dto;

import java.time.LocalDate;

public record SavedTripCreateRequest(
        String title,
        String regionName,
        LocalDate startDate,
        LocalDate endDate,
        String transportMode,
        SavedTripPayload itinerary
) {}
