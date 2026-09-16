/* 단기예보의 발표 시각, 예보 시각, 항목 코드와 값을 표현한다. */
package com.petway.external.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KmaShortForecastItem(
        String baseDate,
        String baseTime,
        String category,
        String fcstDate,
        String fcstTime,
        String fcstValue,
        Integer nx,
        Integer ny
) {
}
