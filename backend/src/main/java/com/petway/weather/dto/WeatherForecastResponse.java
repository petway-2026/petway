package com.petway.weather.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.OutlookLevel;
import com.petway.weather.domain.OutlookProbabilities;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.domain.WeatherForecast;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeatherForecastResponse(
        LocalDate targetDate,
        ForecastType forecastType,
        String regionName,
        Double minimumTemperature,
        Double maximumTemperature,
        Integer rainProbability,
        WeatherCondition weatherCondition,
        WeatherDisplay display,
        OutlookLevel temperatureOutlook,
        OutlookLevel precipitationOutlook,
        OutlookProbabilities temperatureProbabilities,
        OutlookProbabilities precipitationProbabilities,
        LocalDateTime announcedAt,
        String notice,
        String source
) {
    public static WeatherForecastResponse from(WeatherForecast forecast) {
        return new WeatherForecastResponse(
                forecast.targetDate(),
                forecast.forecastType(),
                forecast.regionName(),
                forecast.minimumTemperature(),
                forecast.maximumTemperature(),
                forecast.rainProbability(),
                forecast.weatherCondition(),
                WeatherDisplay.from(forecast),
                forecast.temperatureOutlook(),
                forecast.precipitationOutlook(),
                forecast.temperatureProbabilities(),
                forecast.precipitationProbabilities(),
                forecast.announcedAt(),
                forecast.notice(),
                "기상청"
        );
    }
}
