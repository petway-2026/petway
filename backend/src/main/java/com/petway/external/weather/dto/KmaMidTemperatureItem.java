/* 중기 기온예보에서 대상 날짜의 최저·최고 기온을 선택한다. */
package com.petway.external.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KmaMidTemperatureItem(
        String regId,
        Double taMin4, Double taMax4,
        Double taMin5, Double taMax5,
        Double taMin6, Double taMax6,
        Double taMin7, Double taMax7,
        Double taMin8, Double taMax8,
        Double taMin9, Double taMax9,
        Double taMin10, Double taMax10
) {
    public Double minimumTemperature(int day) {
        return switch (day) {
            case 4 -> taMin4;
            case 5 -> taMin5;
            case 6 -> taMin6;
            case 7 -> taMin7;
            case 8 -> taMin8;
            case 9 -> taMin9;
            case 10 -> taMin10;
            default -> null;
        };
    }

    public Double maximumTemperature(int day) {
        return switch (day) {
            case 4 -> taMax4;
            case 5 -> taMax5;
            case 6 -> taMax6;
            case 7 -> taMax7;
            case 8 -> taMax8;
            case 9 -> taMax9;
            case 10 -> taMax10;
            default -> null;
        };
    }
}
