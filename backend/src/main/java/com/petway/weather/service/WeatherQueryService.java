package com.petway.weather.service;

import com.petway.weather.domain.WeatherForecast;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherForecastResponse;
import com.petway.weather.port.WeatherForecastGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class WeatherQueryService {

    private static final int MAX_FORECAST_DAYS = 90;

    private final WeatherForecastGateway weatherForecastGateway;
    private final Clock weatherClock;
    private final WeatherForecastCache weatherForecastCache;

    public WeatherForecastResponse getForecast(
            double latitude,
            double longitude,
            LocalDate targetDate,
            WeatherRegion region
    ) {
        validateCoordinates(latitude, longitude);
        if (targetDate == null) {
            throw new IllegalArgumentException("여행일은 필수입니다.");
        }
        if (region == null) {
            throw new IllegalArgumentException("날씨 조회에는 region이 필요합니다.");
        }

        LocalDate today = LocalDate.now(weatherClock);
        long days = ChronoUnit.DAYS.between(today, targetDate);
        if (days < 0) {
            throw new IllegalArgumentException("지난 날짜의 예보는 조회할 수 없습니다.");
        }
        if (days > MAX_FORECAST_DAYS) {
            throw new IllegalArgumentException("날씨 전망은 오늘부터 90일 이내만 조회할 수 있습니다.");
        }

        WeatherForecastResponse cached = weatherForecastCache.get(
                latitude,
                longitude,
                targetDate,
                region
        ).orElse(null);
        if (cached != null) {
            return cached;
        }

        WeatherForecast forecast;
        if (days <= 3) {
            forecast = weatherForecastGateway.getShortTerm(latitude, longitude, targetDate, region);
        } else {
            forecast = days <= 10
                    ? weatherForecastGateway.getMidTerm(region, targetDate)
                    : weatherForecastGateway.getSeasonalOutlook(region, targetDate);
        }
        WeatherForecastResponse response = WeatherForecastResponse.from(forecast);
        weatherForecastCache.put(latitude, longitude, targetDate, region, response);
        return response;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("위도는 -90 이상 90 이하여야 합니다.");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("경도는 -180 이상 180 이하여야 합니다.");
        }
    }
}
