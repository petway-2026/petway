package com.petway.global.exception;

import com.petway.external.tour.exception.TourApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.BadCredentialsException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * 한국관광공사 API 연동 실패
     */
    @ExceptionHandler(TourApiException.class)
    public ResponseEntity<?> handleTourApiException(
            TourApiException e
    ) {

        HttpStatus status = switch (e.getErrorType()) {
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case CONFIGURATION -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_GATEWAY;
        };

        return ResponseEntity
                .status(status)
                .body(
                        Map.of(
                                "message",
                                e.getMessage()
                        )
                );
    }


    /**
     * 중복 이메일 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(
            IllegalArgumentException e
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "message",
                                e.getMessage()
                        )
                );
    }


    /**
     * 로그인 실패
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(
            BadCredentialsException e
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        Map.of(
                                "message",
                                "이메일 또는 비밀번호가 올바르지 않습니다."
                        )
                );
    }


    /**
     * Validation 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(
            MethodArgumentNotValidException e
    ) {

        String message =
                e.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getDefaultMessage()
                        )
                        .orElse(
                                "입력값을 확인해주세요."
                        );


        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        Map.of(
                                "message",
                                message
                        )
                );
    }
}
