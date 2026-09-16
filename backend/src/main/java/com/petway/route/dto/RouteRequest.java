/* 방문 순서가 있는 장소 ID와 좌표 목록을 경로 계산에 전달한다. */
package com.petway.route.dto;

import java.util.List;

public record RouteRequest(List<RouteStop> stops) {
    public RouteRequest {
        stops = stops == null ? List.of() : List.copyOf(stops);
    }

    public record RouteStop(
            String id,
            String name,
            double latitude,
            double longitude
    ) {
    }
}
