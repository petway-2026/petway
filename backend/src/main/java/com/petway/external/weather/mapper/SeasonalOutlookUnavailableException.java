/* 요청한 월이나 권역의 계절 전망을 추출할 수 없는 상태를 나타낸다. */
package com.petway.external.weather.mapper;

public class SeasonalOutlookUnavailableException extends IllegalStateException {

    public SeasonalOutlookUnavailableException(String message) {
        super(message);
    }
}
