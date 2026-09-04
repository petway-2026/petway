package com.petway.place.model;

public record PlacePetDetail(
        String accompanimentType,
        String allowedCapacity,
        String requiredItems,
        String additionalInformation,
        String safetyNotice,
        String availableFacilities,
        String providedItems,
        String purchasableItems,
        String rentableItems
) {
    public PlacePetDetail(
            String accompanimentType,
            String allowedCapacity,
            String requiredItems,
            String additionalInformation,
            String safetyNotice
    ) {
        this(
                accompanimentType,
                allowedCapacity,
                requiredItems,
                additionalInformation,
                safetyNotice,
                null,
                null,
                null,
                null
        );
    }

    public static PlacePetDetail empty() {
        return new PlacePetDetail(null, null, null, null, null, null, null, null, null);
    }
}
