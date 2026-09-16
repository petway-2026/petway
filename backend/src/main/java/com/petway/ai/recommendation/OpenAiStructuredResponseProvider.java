/* 구조화된 추천 응답을 요청하는 내부 경계로, 실제 SDK 호출과 테스트용 응답을 교체할 수 있다. */
package com.petway.ai.recommendation;

@FunctionalInterface
interface OpenAiStructuredResponseProvider {
    StructuredRecommendationResponse request(String instructions, String input);
}
