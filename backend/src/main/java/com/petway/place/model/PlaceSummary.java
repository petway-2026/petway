package com.petway.place.model;

import java.math.BigDecimal;

public record PlaceSummary(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String thumbnailUrl
) {
}
