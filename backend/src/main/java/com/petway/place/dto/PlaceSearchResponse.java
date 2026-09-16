/* 검색 페이지, 페이지 크기, 전체 건수와 장소 요약 목록을 묶는 응답이다. */
package com.petway.place.dto;

import java.util.List;

public record PlaceSearchResponse(
        int page,
        int size,
        int totalCount,
        List<PlaceSummaryResponse> items
) {

    public PlaceSearchResponse {
        items = List.copyOf(items);
    }
}
