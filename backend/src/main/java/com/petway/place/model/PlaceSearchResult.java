/* 외부 장소 검색의 페이지 정보와 내부 장소 요약 목록을 묶는다. */
package com.petway.place.model;

import java.util.List;

public record PlaceSearchResult(
        int page,
        int size,
        int totalCount,
        List<PlaceSummary> items
) {

    public PlaceSearchResult {
        items = List.copyOf(items);
    }
}
