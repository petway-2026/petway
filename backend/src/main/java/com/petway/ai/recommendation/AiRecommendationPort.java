package com.petway.ai.recommendation;

public interface AiRecommendationPort {
    RecommendationResult recommend(RecommendationInput input);
}
