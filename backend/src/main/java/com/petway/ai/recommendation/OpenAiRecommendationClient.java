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
        validator.validateInput(input);
        if (provider == null) {
            throw new AiRecommendationUnavailableException();
        }

        try {
            StructuredRecommendationResponse response = provider.request(
                    promptPolicy.instructions(),
                    promptPolicy.createUserPrompt(input)
            );
            return validator.validate(input, response);
        } catch (AiRecommendationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiRecommendationException("OpenAI 추천 요청에 실패했습니다.", exception);
        }
    }
}
