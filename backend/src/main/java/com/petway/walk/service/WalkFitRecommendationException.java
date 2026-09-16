/* 산책 추천을 구성할 수 없는 조회 실패를 상위 HTTP 예외 처리로 전달한다. */
package com.petway.walk.service;

public class WalkFitRecommendationException extends RuntimeException {

    public WalkFitRecommendationException(String message) {
        super(message);
    }
}
