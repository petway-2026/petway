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
