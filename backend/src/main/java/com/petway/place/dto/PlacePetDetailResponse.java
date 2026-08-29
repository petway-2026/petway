package com.petway.place.dto;

public record PlacePetDetailResponse(
        String accompanimentType,
        String allowedCapacity,
        String requiredItems,
        String additionalInformation,
        String safetyNotice
) {
}
