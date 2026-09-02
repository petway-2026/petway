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
