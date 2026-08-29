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
