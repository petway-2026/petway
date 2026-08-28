package com.petway.weather.port;

import com.petway.weather.domain.WeatherForecast;
import com.petway.weather.domain.WeatherRegion;

import java.time.LocalDate;

public interface WeatherForecastGateway {

    WeatherForecast getShortTerm(
            double latitude,
            double longitude,
            LocalDate targetDate,
            WeatherRegion region
    );

    WeatherForecast getMidTerm(WeatherRegion region, LocalDate targetDate);

    WeatherForecast getSeasonalOutlook(WeatherRegion region, LocalDate targetDate);
}
