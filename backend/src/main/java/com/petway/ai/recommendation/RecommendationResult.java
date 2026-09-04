package com.petway.ai.recommendation;

import java.util.List;

public record RecommendationResult(
        String source,
        String summary,
        List<RecommendationItem> recommendations
) {
    public RecommendationResult {
        recommendations = List.copyOf(recommendations);
    }
}
