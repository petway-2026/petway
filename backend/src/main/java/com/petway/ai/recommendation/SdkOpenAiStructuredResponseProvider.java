/* 공식 Java SDK의 구조화 응답을 내부 추천 응답 모델로 받는 어댑터다. */
package com.petway.ai.recommendation;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;

final class SdkOpenAiStructuredResponseProvider implements OpenAiStructuredResponseProvider {
    private final OpenAIClient client;
    private final String model;

    SdkOpenAiStructuredResponseProvider(OpenAIClient client, String model) {
        this.client = client;
        this.model = model;
    }

    @Override
    public StructuredRecommendationResponse request(String instructions, String input) {
        StructuredResponseCreateParams<StructuredRecommendationResponse> params = createParams(
                model,
                instructions,
                input
        );

        return client.responses().create(params).output().stream()
                .flatMap(output -> output.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .findFirst()
                .orElse(null);
    }

    static StructuredResponseCreateParams<StructuredRecommendationResponse> createParams(
            String model,
            String instructions,
            String input
    ) {
        return ResponseCreateParams.builder()
                .instructions(instructions)
                .input(input)
                .text(StructuredRecommendationResponse.class)
                .model(model)
                .store(false)
                .build();
    }
}
