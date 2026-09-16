/* 관광공사 API의 설정, 통신, 호출 제한, 제공자 오류와 응답 형식 오류를 분류한다. */
package com.petway.external.tour.exception;

public enum TourApiErrorType {
    CONFIGURATION,
    CONNECTION,
    TIMEOUT,
    RATE_LIMITED,
    HTTP_ERROR,
    API_ERROR,
    INVALID_RESPONSE
}
