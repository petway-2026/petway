/* 추천 출처, 전체 설명, 순위별 추천 목록을 클라이언트에 반환한다. */
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
