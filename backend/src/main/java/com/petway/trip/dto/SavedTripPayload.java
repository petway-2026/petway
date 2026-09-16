/* 날짜별 방문 순서와 장소·준비물 스냅샷을 표현한다. 빈 목록을 정규화하고 외부 변경을 막는다. */
package com.petway.trip.dto;

import java.time.LocalDate;
import java.util.List;

public record SavedTripPayload(List<DayPlan> days) {
    public SavedTripPayload { days = days == null ? List.of() : List.copyOf(days); }

    public record DayPlan(LocalDate date, List<PlacePlan> places) {
        public DayPlan { places = places == null ? List.of() : List.copyOf(places); }
    }

    public record PlacePlan(
            String contentId,
            String contentTypeId,
            String title,
            String address,
            Double latitude,
            Double longitude,
            String thumbnailUrl,
            List<String> preparations
    ) {
        public PlacePlan {
            preparations = preparations == null ? List.of() : List.copyOf(preparations);
        }
    }
}
