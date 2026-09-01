package com.petway.walk.service;

public class WalkFitRateLimitedException extends WalkFitRecommendationException {

    public WalkFitRateLimitedException(String message) {
        super(message);
    }
}
