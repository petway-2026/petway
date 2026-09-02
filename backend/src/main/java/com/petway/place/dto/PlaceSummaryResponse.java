package com.petway.place.dto;

import java.math.BigDecimal;

public record PlaceSummaryResponse(
        String contentId,
        String contentTypeId,
        String areaCode,
        String sigunguCode,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String thumbnailUrl,
        String telephone,
        String homepage,
        String operatingHours,
        String closedDays,
        String parking,
        String price,
        String indoor,
        String outdoor,
        String description,
        String source
) {
    public PlaceSummaryResponse(String contentId, String contentTypeId, String areaCode, String sigunguCode,
                                String title, String address, BigDecimal latitude, BigDecimal longitude, String thumbnailUrl) {
        this(contentId, contentTypeId, areaCode, sigunguCode, title, address, latitude, longitude, thumbnailUrl,
                null, null, null, null, null, null, null, null, null, "한국관광공사");
    }
}
