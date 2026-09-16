/* 관광공사 API 실패 종류를 보존해 상위 계층에서 오류 응답과 대체 동작을 선택하게 한다. */
package com.petway.external.tour.exception;

import lombok.Getter;

@Getter
public class TourApiException extends RuntimeException {

    private final TourApiErrorType errorType;

    public TourApiException(
            TourApiErrorType errorType,
            String message
    ) {
        super(message);
        this.errorType = errorType;
    }

    public TourApiException(
            TourApiErrorType errorType,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorType = errorType;
    }
}
