/* OpenAiRecommendationClient의 동작을 검증하는 테스트다. 검증 대상: 입력을 검증한 뒤 프롬프트를 구성하고 AI를 호출하며, 반환된 후보 ID와 순위를 다시 검증한다. */
package com.petway.ai.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenAiRecommendationClientTest {
    private final RecommendationPromptPolicy promptPolicy = new RecommendationPromptPolicy(new ObjectMapper());
    private final RecommendationValidator validator = new RecommendationValidator();

    @Test
    void compactPromptOmitsEmptyDataButRetainsAdmissionConditions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RecommendationInput input = new RecommendationInput(
                "서울 2일 여행", new RecommendationPetSnapshot("소형견"), "맑음",
                java.util.List.of(
                        new RecommendationCandidate("available", "공원", "관광지",
                                CandidateCompatibilityStatus.AVAILABLE, java.util.List.of(),
                                java.util.List.of(), 1.2, null),
                        RecommendationFixtures.candidate("confirmation", CandidateCompatibilityStatus.NEED_CONFIRMATION),
                        RecommendationFixtures.candidate("blocked", CandidateCompatibilityStatus.UNAVAILABLE)
                )
        );
        String prompt = promptPolicy.createUserPrompt(input);
        var candidates = mapper.readTree(prompt.substring(prompt.indexOf('{'))).get("candidates");
        assertThat(candidates.size()).isEqualTo(2);
        assertThat(candidates.get(0).has("status")).isFalse();
        assertThat(candidates.get(0).has("requirements")).isFalse();
        assertThat(candidates.get(0).has("weather")).isFalse();
        assertThat(candidates.get(0).get("km").asDouble()).isEqualTo(1.2);
        assertThat(candidates.get(1).get("status").asText()).isEqualTo("NEED_CONFIRMATION");
        assertThat(candidates.get(1).get("requirements").get(0).asText()).isEqualTo("방문 전 전화 확인");
        assertThat(prompt).doesNotContain("blocked");
    }

    @Test
    void returnsValidatedStructuredRecommendation() {
        OpenAiRecommendationClient client = new OpenAiRecommendationClient(
                promptPolicy,
                validator,
                (instructions, input) -> RecommendationFixtures.validResponse()
        );

        RecommendationResult result = client.recommend(RecommendationFixtures.validInput());

        assertThat(result.source()).isEqualTo("OPENAI");
        assertThat(result.recommendations()).hasSize(2);
        assertThat(result.recommendations().get(1).requiresConfirmation()).isTrue();
    }

    @Test
    void excludesUnavailableCandidatesFromProviderPrompt() {
        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        OpenAiRecommendationClient client = new OpenAiRecommendationClient(
                promptPolicy,
                validator,
                (instructions, input) -> {
                    capturedPrompt.set(input);
                    return RecommendationFixtures.validResponse();
                }
        );

        client.recommend(RecommendationFixtures.validInput());

        assertThat(capturedPrompt.get())
                .contains("place-1", "place-2", "place-4", "CONDITIONAL")
                .doesNotContain("place-3", "UNAVAILABLE");
    }

    @Test
    void recommendsConditionalCandidateAndPreservesItsStatus() {
        OpenAiRecommendationClient client = new OpenAiRecommendationClient(
                promptPolicy,
                validator,
                (instructions, input) -> {
                    assertThat(input).contains("place-4", "CONDITIONAL");
                    return RecommendationFixtures.responseWith(
                            RecommendationFixtures.item("place-4", 1, "준비사항을 지키면 방문할 수 있습니다.")
                    );
                }
        );

        RecommendationItem item = client.recommend(RecommendationFixtures.validInput())
                .recommendations().get(0);

        assertThat(item.compatibilityStatus()).isEqualTo(CandidateCompatibilityStatus.CONDITIONAL);
        assertThat(item.requiresConfirmation()).isFalse();
    }

    @Test
    void wrapsProviderFailureWithoutExposingInput() {
        OpenAiRecommendationClient client = new OpenAiRecommendationClient(
                promptPolicy,
                validator,
                (instructions, input) -> {
                    throw new IllegalStateException("provider unavailable");
                }
        );

        assertThatThrownBy(() -> client.recommend(RecommendationFixtures.validInput()))
                .isInstanceOf(AiRecommendationException.class)
                .hasMessage("OpenAI 추천 요청에 실패했습니다.");
    }

    @Test
    void portCanBeReplacedWithFake() {
        AiRecommendationPort fake = input -> new RecommendationResult("OPENAI", "fake", java.util.List.of());

        assertThat(fake.recommend(RecommendationFixtures.validInput()).summary()).isEqualTo("fake");
    }

    @Test
    void officialSdkCanBuildStructuredResponsesSchemaLocally() {
        StructuredResponseCreateParams<StructuredRecommendationResponse> params =
                SdkOpenAiStructuredResponseProvider.createParams(
                        "gpt-5.4-mini",
                        "instructions",
                        "test"
                );

        assertThat(params.responseType()).isEqualTo(StructuredRecommendationResponse.class);
        assertThat(params.rawParams().store()).contains(false);
    }
}
