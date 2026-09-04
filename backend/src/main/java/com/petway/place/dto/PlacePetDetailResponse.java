package com.petway.place.dto;

import com.petway.place.model.PlacePetDetail;

public record PlacePetDetailResponse(
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
    public PlacePetDetailResponse(
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

    public static PlacePetDetailResponse from(PlacePetDetail detail) {
        PlacePetDetail safeDetail = detail == null
                ? PlacePetDetail.empty()
                : detail;

        return new PlacePetDetailResponse(
                safeDetail.accompanimentType(),
                safeDetail.allowedCapacity(),
                safeDetail.requiredItems(),
                safeDetail.additionalInformation(),
                safeDetail.safetyNotice(),
                safeDetail.availableFacilities(),
                safeDetail.providedItems(),
                safeDetail.purchasableItems(),
                safeDetail.rentableItems()
        );
    }
}
