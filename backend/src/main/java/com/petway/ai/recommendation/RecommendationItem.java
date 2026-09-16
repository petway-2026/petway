/* 검증을 마친 후보별 순위와 추천 이유, 준비사항이다. 동반 상태는 입력 후보의 값을 사용한다. */
package com.petway.ai.recommendation;

import java.util.List;

public record RecommendationItem(
        String candidateId,
        int rank,
        String reason,
        List<String> preparationTips,
        CandidateCompatibilityStatus compatibilityStatus
) {
    public RecommendationItem {
        preparationTips = List.copyOf(preparationTips);
    }

    public boolean requiresConfirmation() {
        return compatibilityStatus == CandidateCompatibilityStatus.NEED_CONFIRMATION;
    }
}
