/* 장소 ID·유형·지역·이름·좌표·이미지를 담는 가벼운 내부 검색 결과다. */
package com.petway.place.model;

import java.math.BigDecimal;

public record PlaceSummary(
        String contentId,
        String contentTypeId,
        String areaCode,
        String sigunguCode,
        String title,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String thumbnailUrl
) {
}
