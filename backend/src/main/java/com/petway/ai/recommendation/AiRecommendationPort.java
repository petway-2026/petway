/* 여행 추천의 입력과 결과 계약이다. HTTP 계층이 특정 AI SDK 구현에 직접 의존하지 않게 한다. */
package com.petway.ai.recommendation;

public interface AiRecommendationPort {
    RecommendationResult recommend(RecommendationInput input);
}
