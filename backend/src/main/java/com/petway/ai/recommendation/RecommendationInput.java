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
