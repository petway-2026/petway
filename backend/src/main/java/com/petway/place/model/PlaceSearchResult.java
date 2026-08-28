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
