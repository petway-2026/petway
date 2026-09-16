/* AI 추천 테스트에서 공통으로 사용할 반려견·후보·입력 예시를 생성한다. */
package com.petway.ai.recommendation;

import java.util.List;

final class RecommendationFixtures {
    private RecommendationFixtures() {
    }

    static RecommendationInput validInput() {
        return new RecommendationInput(
                "비 오는 날 반려견과 조용히 방문할 곳",
                new RecommendationPetSnapshot("소형견, 낯선 개를 조금 경계함"),
                "약한 비, 18도",
                List.of(
                        candidate("place-1", CandidateCompatibilityStatus.AVAILABLE),
                        candidate("place-2", CandidateCompatibilityStatus.NEED_CONFIRMATION),
                        candidate("place-3", CandidateCompatibilityStatus.UNAVAILABLE),
                        candidate("place-4", CandidateCompatibilityStatus.CONDITIONAL)
                )
        );
    }

    static RecommendationCandidate candidate(String id, CandidateCompatibilityStatus status) {
        return new RecommendationCandidate(
                id,
                "후보 " + id,
                "실내",
                status,
                List.of("반려동물 동반 정보 확인됨"),
                status == CandidateCompatibilityStatus.NEED_CONFIRMATION
                        ? List.of("방문 전 전화 확인")
                        : List.of(),
                2.5,
                "실내 활동에 적합"
        );
    }

    static StructuredRecommendationResponse validResponse() {
        return responseWith(
                item("place-1", 1, "날씨 영향을 적게 받습니다."),
                item("place-2", 2, "방문 전 동반 가능 여부를 확인하세요.")
        );
    }

    static StructuredRecommendationResponse responseWith(
            StructuredRecommendationResponse.StructuredRecommendationItem... items
    ) {
        StructuredRecommendationResponse response = new StructuredRecommendationResponse();
        response.summary = "검증된 후보 중 두 곳을 추천합니다.";
        response.recommendations = List.of(items);
        return response;
    }

    static StructuredRecommendationResponse.StructuredRecommendationItem item(
            String candidateId,
            int rank,
            String reason
    ) {
        StructuredRecommendationResponse.StructuredRecommendationItem item =
                new StructuredRecommendationResponse.StructuredRecommendationItem();
        item.candidateId = candidateId;
        item.rank = rank;
        item.reason = reason;
        item.preparationTips = List.of("이동 전 운영시간 확인");
        return item;
    }
}
