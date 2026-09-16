/* 단기·중기·계절 전망 조회를 정의해 기상청 통신 구현을 서비스와 분리한다. */
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
