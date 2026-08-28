package com.petway.ai.recommendation;

public class AiRecommendationUnavailableException extends AiRecommendationException {
    public AiRecommendationUnavailableException() {
        super("OpenAI 추천 기능이 설정되지 않았습니다.");
    }
}
