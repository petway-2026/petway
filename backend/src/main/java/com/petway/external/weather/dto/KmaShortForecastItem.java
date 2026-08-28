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
