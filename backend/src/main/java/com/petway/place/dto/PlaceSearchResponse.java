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
