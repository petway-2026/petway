/* 외부 제공자의 동반 상세 원문을 보관하는 내부 모델이다. 파싱 전 원문과 판정 결과를 분리한다. */
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
