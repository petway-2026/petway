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
