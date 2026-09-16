/* 동물등록 제공자 오류의 분류와 원인을 서비스·예외 처리 계층에 전달한다. */
package com.petway.external.animalregistration.exception;

public class AnimalRegistrationException extends RuntimeException {

    private final AnimalRegistrationErrorType errorType;

    public AnimalRegistrationException(
            AnimalRegistrationErrorType errorType,
            String message
    ) {
        super(message);
        this.errorType = errorType;
    }

    public AnimalRegistrationException(
            AnimalRegistrationErrorType errorType,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorType = errorType;
    }

    public AnimalRegistrationErrorType getErrorType() {
        return errorType;
    }
}
