/* 산책 후보의 동반정보 조회가 모두 제공자 호출 제한에 막힌 경우를 구분한다. */
package com.petway.walk.service;

public class WalkFitRateLimitedException extends WalkFitRecommendationException {

    public WalkFitRateLimitedException(String message) {
        super(message);
    }
}
