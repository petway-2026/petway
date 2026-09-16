/* 기상 제공자 호출이나 응답 처리 실패를 공통으로 전달한다. */
package com.petway.weather.service;

public class WeatherProviderException extends RuntimeException {

    public WeatherProviderException(String message) {
        super(message);
    }

    public WeatherProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
