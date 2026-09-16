/* 대중교통 구간별 시간·요금·환승 및 도보·버스·철도 이동 단계를 반환한다. */
package com.petway.route.dto;

import java.util.List;

public record TransitRouteResponse(
        int totalDurationMinutes,
        int totalFare,
        List<TransitLeg> legs
) {
    public record TransitLeg(
            String fromId,
            String toId,
            int durationMinutes,
            int fare,
            int transfers,
            List<TransitStep> steps
    ) {
    }

    public record TransitStep(
            String type,
            int durationMinutes,
            int distanceMeters,
            String description
    ) {
    }
}
