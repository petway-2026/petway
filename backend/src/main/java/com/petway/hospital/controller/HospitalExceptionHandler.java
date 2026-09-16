/* 병원 검색의 제공자 실패와 잘못된 요청을 HTTP 오류로 변환한다. */
package com.petway.hospital.controller;

import com.petway.hospital.client.AnimalHospitalApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = HospitalController.class)
public class HospitalExceptionHandler {

    @ExceptionHandler(AnimalHospitalApiException.class)
    public ResponseEntity<Map<String, String>> handleExternalApiFailure(
            AnimalHospitalApiException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", exception.getMessage()));
    }
}
