package com.petway.ai.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationValidatorTest {
    private final RecommendationValidator validator = new RecommendationValidator();

    @Test
    void acceptsValidRecommendationInput() {
        validator.validateInput(RecommendationFixtures.validInput());
    }

    @Test
    void acceptsValidStructuredResult() {
        RecommendationResult result = validator.validate(
                RecommendationFixtures.validInput(),
                RecommendationFixtures.validResponse()
        );

        assertThat(result.summary()).isEqualTo("검증된 후보 중 두 곳을 추천합니다.");
        assertThat(result.recommendations()).extracting(RecommendationItem::candidateId)
                .containsExactly("place-1", "place-2");
    }

    @Test
    void rejectsUnknownCandidateId() {
        StructuredRecommendationResponse response = responseWith(
                RecommendationFixtures.item("unknown", 1, "추천 이유")
        );

        assertInvalid(response, "입력에 없는 candidateId");
    }

    @Test
    void rejectsDuplicateCandidateId() {
        StructuredRecommendationResponse response = responseWith(
                RecommendationFixtures.item("place-1", 1, "첫 번째 이유"),
                RecommendationFixtures.item("place-1", 2, "두 번째 이유")
        );

        assertInvalid(response, "candidateId가 중복");
    }

    @Test
    void rejectsUnavailableCandidate() {
        StructuredRecommendationResponse response = responseWith(
                RecommendationFixtures.item("place-3", 1, "추천 이유")
        );

        assertInvalid(response, "UNAVAILABLE 후보");
    }

    @Test
    void preservesNeedConfirmationStatusFromInput() {
        StructuredRecommendationResponse response = responseWith(
                RecommendationFixtures.item("place-2", 1, "전화 확인 후 방문하세요.")
        );

        RecommendationItem item = validator.validate(RecommendationFixtures.validInput(), response)
                .recommendations().get(0);

        assertThat(item.compatibilityStatus()).isEqualTo(CandidateCompatibilityStatus.NEED_CONFIRMATION);
        assertThat(item.requiresConfirmation()).isTrue();
    }

    @Test
    void rejectsEmptyStructuredResponse() {
        StructuredRecommendationResponse response = new StructuredRecommendationResponse();
        response.summary = "요약";
        response.recommendations = List.of();

        assertInvalid(response, "추천 목록이 비어");
    }

    @Test
    void rejectsNullStructuredResponse() {
        assertThatThrownBy(() -> validator.validate(RecommendationFixtures.validInput(), null))
                .isInstanceOf(InvalidRecommendationException.class)
                .hasMessageContaining("비어 있거나 올바르지");
    }

    @Test
    void rejectsInvalidRank() {
        StructuredRecommendationResponse response = responseWith(
                RecommendationFixtures.item("place-1", 2, "추천 이유")
        );

        assertInvalid(response, "순위가 올바르지");
    }

    @Test
    void rejectsDuplicateInputCandidateId() {
        RecommendationCandidate duplicate = RecommendationFixtures.candidate(
                "duplicate",
                CandidateCompatibilityStatus.AVAILABLE
        );
        RecommendationInput input = new RecommendationInput(
                "사용자 의도",
                new RecommendationPetSnapshot("반려동물 요약"),
                "날씨 요약",
                List.of(duplicate, duplicate)
        );

        assertThatThrownBy(() -> validator.validateInput(input))
                .isInstanceOf(InvalidRecommendationException.class)
                .hasMessageContaining("입력 후보 candidateId가 중복");
    }

    @Test
    void rejectsDuplicateOutputRank() {
        StructuredRecommendationResponse response = responseWith(
                RecommendationFixtures.item("place-1", 1, "첫 번째 이유"),
                RecommendationFixtures.item("place-2", 1, "두 번째 이유")
        );

        assertInvalid(response, "순위가 올바르지");
    }

    @Test
    void rejectsBlankReason() {
        assertInvalid(
                responseWith(RecommendationFixtures.item("place-1", 1, " ")),
                "추천 설명이 올바르지"
        );
    }

    @Test
    void rejectsNullPreparationTips() {
        StructuredRecommendationResponse.StructuredRecommendationItem item =
                RecommendationFixtures.item("place-1", 1, "추천 이유");
        item.preparationTips = null;

        assertInvalid(responseWith(item), "추천 설명이 올바르지");
    }

    @Test
    void rejectsBlankSummary() {
        StructuredRecommendationResponse response = responseWith(
                RecommendationFixtures.item("place-1", 1, "추천 이유")
        );
        response.summary = " ";

        assertInvalid(response, "비어 있거나 올바르지");
    }

    @Test
    void rejectsNullRecommendations() {
        StructuredRecommendationResponse response = new StructuredRecommendationResponse();
        response.summary = "요약";
        response.recommendations = null;

        assertInvalid(response, "추천 목록이 비어");
    }

    private StructuredRecommendationResponse responseWith(
            StructuredRecommendationResponse.StructuredRecommendationItem... items
    ) {
        StructuredRecommendationResponse response = new StructuredRecommendationResponse();
        response.summary = "요약";
        response.recommendations = List.of(items);
        return response;
    }

    private void assertInvalid(StructuredRecommendationResponse response, String message) {
        assertThatThrownBy(() -> validator.validate(RecommendationFixtures.validInput(), response))
                .isInstanceOf(InvalidRecommendationException.class)
                .hasMessageContaining(message);
    }
}
