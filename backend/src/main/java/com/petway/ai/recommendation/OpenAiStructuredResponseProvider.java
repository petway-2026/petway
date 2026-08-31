package com.petway.ai.recommendation;

@FunctionalInterface
interface OpenAiStructuredResponseProvider {
    StructuredRecommendationResponse request(String instructions, String input);
}
