/* 기상 제공자 응답 시간 초과를 일반 통신 실패와 구분한다. */
package com.petway.weather.service;

public class WeatherProviderTimeoutException extends WeatherProviderException {

    public WeatherProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
