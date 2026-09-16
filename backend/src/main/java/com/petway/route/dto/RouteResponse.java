/* 자동차 경로의 총 거리·시간, 구간별 정보와 지도에 그릴 좌표를 반환한다. */
package com.petway.route.dto;

import java.util.List;

public record RouteResponse(
        int totalDistanceMeters,
        int totalDurationSeconds,
        List<RouteLeg> legs,
        List<RoutePoint> path
) {
    public record RouteLeg(
            String fromId,
            String toId,
            int distanceMeters,
            int durationSeconds
    ) {
    }

    public record RoutePoint(double latitude, double longitude) {
    }
}
