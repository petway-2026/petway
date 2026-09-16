/* 동물등록 조회의 미조회, 설정, 통신, 시간 초과, 응답 오류를 구분한다. */
package com.petway.external.animalregistration.exception;

public enum AnimalRegistrationErrorType {
    CONFIGURATION,
    TIMEOUT,
    CONNECTION,
    HTTP_ERROR,
    API_ERROR,
    INVALID_RESPONSE,
    NOT_FOUND
}
