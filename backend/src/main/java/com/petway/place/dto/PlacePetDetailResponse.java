/* 동반 유형·필요사항·시설·대여 품목 등 공식 동반 상세 필드를 응답으로 전달한다. */
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
