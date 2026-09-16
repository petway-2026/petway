/* 일별 예보와 계절 전망을 함께 표현하는 내부 날씨 모델이다. */
package com.petway.weather.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeatherForecast(
        LocalDate targetDate,
        ForecastType forecastType,
        String regionName,
        Double minimumTemperature,
        Double maximumTemperature,
        Integer rainProbability,
        WeatherCondition weatherCondition,
        OutlookLevel temperatureOutlook,
        OutlookLevel precipitationOutlook,
        OutlookProbabilities temperatureProbabilities,
        OutlookProbabilities precipitationProbabilities,
        LocalDateTime announcedAt,
        String notice
) {
}
