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
