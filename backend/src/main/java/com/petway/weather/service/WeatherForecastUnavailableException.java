package com.petway.weather.service;

public class WeatherForecastUnavailableException extends RuntimeException {

    public WeatherForecastUnavailableException(String message) {
        super(message);
    }
}
