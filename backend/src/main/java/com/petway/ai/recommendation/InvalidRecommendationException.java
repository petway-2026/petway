/* 후보 입력이나 AI 출력이 추천 계약에 맞지 않을 때 발생하는 예외다. */
package com.petway.ai.recommendation;

public class InvalidRecommendationException extends AiRecommendationException {
    public InvalidRecommendationException(String message) {
        super(message);
    }
}
