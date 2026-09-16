/* 저장된 여행의 식별자, 요약 정보, 날짜별 일정 및 생성 시각을 반환한다. */
package com.petway.trip.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SavedTripResponse(
        Long id,
        String title,
        String regionName,
        LocalDate startDate,
        LocalDate endDate,
        String transportMode,
        int placeCount,
        SavedTripPayload itinerary,
        LocalDateTime createdAt
) {}
