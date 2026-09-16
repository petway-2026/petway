/* 좌표·날짜를 검증하고 방문일까지 남은 기간에 맞는 예보를 선택하며 성공 응답을 캐시한다. */
package com.petway.weather.service;

import com.petway.weather.domain.ForecastType;
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
        ForecastType forecastType = resolveForecastType(targetDate);
        if (region == null) {
            throw new IllegalArgumentException("날씨 조회에는 region이 필요합니다.");
        }

        WeatherForecastResponse cached = weatherForecastCache.get(
                latitude,
                longitude,
                targetDate,
                region,
                forecastType
        ).orElse(null);
        if (cached != null) {
            return cached;
        }

        WeatherForecast forecast;
        if (forecastType == ForecastType.SHORT_TERM) {
            forecast = weatherForecastGateway.getShortTerm(latitude, longitude, targetDate, region);
        } else {
            forecast = forecastType == ForecastType.MID_TERM
                    ? weatherForecastGateway.getMidTerm(region, targetDate)
                    : weatherForecastGateway.getSeasonalOutlook(region, targetDate);
        }
        WeatherForecastResponse response = WeatherForecastResponse.from(forecast);
        // 정상 조회·변환이 완료된 응답만 캐시에 넣는다. 조회 실패는 이후 요청에서 다시 시도한다.
        weatherForecastCache.put(latitude, longitude, targetDate, region, forecastType, response);
        return response;
    }

    public ForecastType resolveForecastType(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("여행일은 필수입니다.");
        }
        LocalDate today = LocalDate.now(weatherClock);
        long days = ChronoUnit.DAYS.between(today, targetDate);
        if (days < 0) {
            throw new IllegalArgumentException("지난 날짜의 예보는 조회할 수 없습니다.");
        }
        if (days > MAX_FORECAST_DAYS) {
            throw new IllegalArgumentException("날씨 전망은 오늘부터 90일 이내만 조회할 수 있습니다.");
        }
        return forecastType(days);
    }

    // 오늘~3일은 단기, 4~10일은 중기, 이후 허용 범위는 계절 전망으로 분기한다.
    private ForecastType forecastType(long days) {
        if (days <= 3) {
            return ForecastType.SHORT_TERM;
        }
        return days <= 10 ? ForecastType.MID_TERM : ForecastType.SEASONAL_OUTLOOK;
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
