package com.petway.place.model;

public record PlaceSearchCondition(
        String areaCode,
        String sigunguCode,
        String contentTypeId,
        int page,
        int size
) {
}
