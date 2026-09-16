/* 외부 조회와 캐시에 전달하는 좌표·반경·페이지 검색 조건이다. */
package com.petway.place.model;

import java.math.BigDecimal;

public record NearbyPlaceSearchCondition(
        BigDecimal latitude,
        BigDecimal longitude,
        int radius,
        int page,
        int size
) {
}
