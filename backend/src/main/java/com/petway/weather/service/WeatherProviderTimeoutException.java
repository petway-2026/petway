package com.petway.weather.service;

public class WeatherProviderTimeoutException extends WeatherProviderException {

    public WeatherProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
