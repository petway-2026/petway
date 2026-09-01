package com.petway.place.dto;

import com.petway.place.model.PlacePetDetail;

public record PlacePetDetailResponse(
        String accompanimentType,
        String allowedCapacity,
        String requiredItems,
        String additionalInformation,
        String safetyNotice
) {
    public static PlacePetDetailResponse from(PlacePetDetail detail) {
        PlacePetDetail safeDetail = detail == null
                ? PlacePetDetail.empty()
                : detail;

        return new PlacePetDetailResponse(
                safeDetail.accompanimentType(),
                safeDetail.allowedCapacity(),
                safeDetail.requiredItems(),
                safeDetail.additionalInformation(),
                safeDetail.safetyNotice()
        );
    }
}
