/* 추천 요청에 포함된 장소의 ID, 동반 판정, 준비사항, 거리와 날씨 스냅샷이다. */
package com.petway.ai.recommendation;

import java.util.List;

public record RecommendationCandidate(
        String candidateId,
        String name,
        String category,
        CandidateCompatibilityStatus compatibilityStatus,
        List<String> compatibilityReasons,
        List<String> requirements,
        Double distanceKm,
        String weatherSummary
) {
    public RecommendationCandidate {
        compatibilityReasons = compatibilityReasons == null ? null : List.copyOf(compatibilityReasons);
        requirements = requirements == null ? null : List.copyOf(requirements);
    }
}
