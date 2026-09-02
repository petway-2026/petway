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
