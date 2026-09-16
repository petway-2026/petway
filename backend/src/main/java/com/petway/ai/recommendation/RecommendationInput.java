/* 사용자 여행 의도, 반려견 요약, 날씨와 추천 후보를 묶는 입력이다. */
package com.petway.ai.recommendation;

import java.util.List;

public record RecommendationInput(
        String userIntent,
        RecommendationPetSnapshot pet,
        String weatherSummary,
        List<RecommendationCandidate> candidates
) {
    public RecommendationInput {
        candidates = candidates == null ? null : List.copyOf(candidates);
    }
}
