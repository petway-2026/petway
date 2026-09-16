/* 지역 코드, 시군구, 장소 유형, 페이지로 구성된 내부 검색 조건이다. */
package com.petway.place.model;

public record PlaceSearchCondition(
        String areaCode,
        String sigunguCode,
        String contentTypeId,
        int page,
        int size
) {
}
