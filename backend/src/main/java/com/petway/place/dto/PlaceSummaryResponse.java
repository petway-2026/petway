package com.petway.place.dto;

import java.math.BigDecimal;

public record PlaceSummaryResponse(
        String contentId,
        String contentTypeId,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String thumbnailUrl
) {
}
