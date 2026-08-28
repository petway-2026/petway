package com.petway.ai.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(OpenAiRecommendationContextTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "OPENAI_API_KEY=",
        "OPENAI_MODEL=test-model-override",
        "OPENAI_TIMEOUT_SECONDS=45"
})
class OpenAiRecommendationContextTest {
    @Autowired
    private AiRecommendationPort recommendationPort;

    @Autowired
    private OpenAiRecommendationProperties properties;

    @Test
    void springContextStartsWithoutApiKeyAndCallFailsSafely() {
        assertThat(recommendationPort).isInstanceOf(OpenAiRecommendationClient.class);
        assertThatThrownBy(() -> recommendationPort.recommend(RecommendationFixtures.validInput()))
                .isInstanceOf(AiRecommendationUnavailableException.class)
                .hasMessage("OpenAI 추천 기능이 설정되지 않았습니다.");
        assertThat(properties.model()).isEqualTo("test-model-override");
        assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(45));
    }

    @Configuration
    @Import({
            OpenAiRecommendationProperties.class,
            RecommendationPromptPolicy.class,
            RecommendationValidator.class,
            OpenAiRecommendationClient.class
    })
    static class TestConfiguration {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
