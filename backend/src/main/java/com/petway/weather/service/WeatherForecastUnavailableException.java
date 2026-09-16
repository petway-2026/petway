/* 요청한 조건에 제공할 예보가 없는 경우를 나타낸다. */
package com.petway.weather.service;

public class WeatherForecastUnavailableException extends RuntimeException {

    public WeatherForecastUnavailableException(String message) {
        super(message);
    }
}
