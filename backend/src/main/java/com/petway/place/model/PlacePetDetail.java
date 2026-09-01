package com.petway.place.model;

public record PlacePetDetail(
        String accompanimentType,
        String allowedCapacity,
        String requiredItems,
        String additionalInformation,
        String safetyNotice
) {
    public static PlacePetDetail empty() {
        return new PlacePetDetail(null, null, null, null, null);
    }
}
