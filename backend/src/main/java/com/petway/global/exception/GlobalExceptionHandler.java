package com.petway.global.exception;

import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.weather.service.WeatherForecastUnavailableException;
import com.petway.weather.service.WeatherProviderException;
import com.petway.weather.service.WeatherProviderTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WeatherForecastUnavailableException.class)
    public ResponseEntity<?> handleWeatherUnavailable(WeatherForecastUnavailableException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(WeatherProviderTimeoutException.class)
    public ResponseEntity<?> handleWeatherProviderTimeout(WeatherProviderTimeoutException e) {
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(Map.of("message", "기상청 날씨 정보 응답 시간이 초과되었습니다."));
    }

    @ExceptionHandler(WeatherProviderException.class)
    public ResponseEntity<?> handleWeatherProvider(WeatherProviderException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "기상청 날씨 정보 호출에 실패했습니다."));
    }

    @ExceptionHandler(TourApiException.class)
    public ResponseEntity<?> handleTourApiException(
            TourApiException e
    ) {
        HttpStatus status = switch (e.getErrorType()) {
            case CONFIGURATION -> HttpStatus.SERVICE_UNAVAILABLE;
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case CONNECTION, HTTP_ERROR, API_ERROR -> HttpStatus.BAD_GATEWAY;
            case INVALID_RESPONSE -> HttpStatus.BAD_GATEWAY;
        };

        return ResponseEntity
                .status(status)
                .body(Map.of("message", e.getMessage()));
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

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> handleDisabledAccount(
            DisabledException e
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "비활성화된 계정입니다."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(
            IllegalStateException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", e.getMessage()));
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
