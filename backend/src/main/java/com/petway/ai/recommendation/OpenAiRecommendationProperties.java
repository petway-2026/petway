package com.petway.ai.recommendation;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiRecommendationProperties {
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiRecommendationProperties(
            @Value("${OPENAI_API_KEY:}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-5.4-mini}") String model,
            @Value("${OPENAI_TIMEOUT_SECONDS:30}") long timeoutSeconds
    ) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("OPENAI_TIMEOUT_SECONDS는 1 이상이어야 합니다.");
        }
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-5.4-mini" : model.trim();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    String apiKey() {
        return apiKey;
    }

    String model() {
        return model;
    }

    Duration timeout() {
        return timeout;
    }
}
