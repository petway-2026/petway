/* 방문일까지 남은 기간에 따라 단기예보·중기예보·계절 전망을 구분한다. */
package com.petway.weather.domain;

public enum ForecastType {
    SHORT_TERM,
    MID_TERM,
    SEASONAL_OUTLOOK
}
