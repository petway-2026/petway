/* 제공자별 하늘·강수 표현을 서비스에서 공통으로 사용할 날씨 상태로 정의한다. */
package com.petway.weather.domain;

public enum WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    RAIN_OR_SNOW,
    SNOW,
    SHOWER,
    UNKNOWN
}
