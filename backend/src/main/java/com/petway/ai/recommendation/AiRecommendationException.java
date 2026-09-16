/* AI 추천 처리 실패를 전달하는 공통 예외이며, 원인 예외를 함께 보존할 수 있다. */
package com.petway.ai.recommendation;

public class AiRecommendationException extends RuntimeException {
    public AiRecommendationException(String message) {
        super(message);
    }

    public AiRecommendationException(String message, Throwable cause) {
        super(message, cause);
    }
}
