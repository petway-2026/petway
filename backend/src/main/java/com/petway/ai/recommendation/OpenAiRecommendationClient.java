/* 입력을 검증한 뒤 프롬프트를 구성하고 AI를 호출하며, 반환된 후보 ID와 순위를 다시 검증한다. */
package com.petway.ai.recommendation;

import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenAiRecommendationClient implements AiRecommendationPort {
    private final RecommendationPromptPolicy promptPolicy;
    private final RecommendationValidator validator;
    private final OpenAiStructuredResponseProvider provider;

    @Autowired
    public OpenAiRecommendationClient(
            OpenAiRecommendationProperties properties,
            RecommendationPromptPolicy promptPolicy,
            RecommendationValidator validator
    ) {
        this(
                promptPolicy,
                validator,
                properties.isConfigured()
                        ? new SdkOpenAiStructuredResponseProvider(
                                OpenAIOkHttpClient.builder()
                                        .apiKey(properties.apiKey())
                                        .timeout(properties.timeout())
                                        .build(),
                                properties.model()
                        )
                        : null
        );
    }

    OpenAiRecommendationClient(
            RecommendationPromptPolicy promptPolicy,
            RecommendationValidator validator,
            OpenAiStructuredResponseProvider provider
    ) {
        this.promptPolicy = promptPolicy;
        this.validator = validator;
        this.provider = provider;
    }

    @Override
    public RecommendationResult recommend(RecommendationInput input) {
        // 외부 호출 전에 필수값과 후보 계약을 확인해 잘못된 입력으로 모델을 호출하지 않는다.
        validator.validateInput(input);
        if (provider == null) {
            throw new AiRecommendationUnavailableException();
        }

        try {
            StructuredRecommendationResponse response = provider.request(
                    promptPolicy.instructions(),
                    promptPolicy.createUserPrompt(input)
            );
            // 구조화 응답도 신뢰된 결과로 바로 사용하지 않고 입력 후보와 대조해 검증한다.
            return validator.validate(input, response);
        } catch (AiRecommendationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiRecommendationException("OpenAI 추천 요청에 실패했습니다.", exception);
        }
    }
}
