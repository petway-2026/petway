/* API 키가 없어 AI 추천 제공자를 구성하지 못한 상태를 나타낸다. */
package com.petway.ai.recommendation;

public class AiRecommendationUnavailableException extends AiRecommendationException {
    public AiRecommendationUnavailableException() {
        super("OpenAI 추천 기능이 설정되지 않았습니다.");
    }
}
