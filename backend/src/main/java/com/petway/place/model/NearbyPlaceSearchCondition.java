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
