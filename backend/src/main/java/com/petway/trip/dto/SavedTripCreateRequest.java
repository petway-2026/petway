/* 여행 제목·지역·날짜·이동수단과 날짜별 장소 구성을 저장 요청으로 받는다. */
package com.petway.trip.dto;

import java.time.LocalDate;

public record SavedTripCreateRequest(
        String title,
        String regionName,
        LocalDate startDate,
        LocalDate endDate,
        String transportMode,
        SavedTripPayload itinerary
) {}
