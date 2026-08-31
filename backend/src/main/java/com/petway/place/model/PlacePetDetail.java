package com.petway.place.model;

public record PlacePetDetail(
        String accompanimentType,
        String allowedCapacity,
        String requiredItems,
        String additionalInformation,
        String safetyNotice
) {
}
