/* 중기 육상예보의 날짜별 강수확률과 날씨 설명을 읽는 응답 모델이다. */
package com.petway.external.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KmaMidLandItem(
        String regId,
        Integer rnSt4Am, Integer rnSt4Pm, String wf4Am, String wf4Pm,
        Integer rnSt5Am, Integer rnSt5Pm, String wf5Am, String wf5Pm,
        Integer rnSt6Am, Integer rnSt6Pm, String wf6Am, String wf6Pm,
        Integer rnSt7Am, Integer rnSt7Pm, String wf7Am, String wf7Pm,
        Integer rnSt8, String wf8,
        Integer rnSt9, String wf9,
        Integer rnSt10, String wf10
) {
    public Integer rainProbability(int day) {
        return switch (day) {
            case 4 -> max(rnSt4Am, rnSt4Pm);
            case 5 -> max(rnSt5Am, rnSt5Pm);
            case 6 -> max(rnSt6Am, rnSt6Pm);
            case 7 -> max(rnSt7Am, rnSt7Pm);
            case 8 -> rnSt8;
            case 9 -> rnSt9;
            case 10 -> rnSt10;
            default -> null;
        };
    }

    public String weatherDescription(int day) {
        return switch (day) {
            case 4 -> combine(wf4Am, wf4Pm);
            case 5 -> combine(wf5Am, wf5Pm);
            case 6 -> combine(wf6Am, wf6Pm);
            case 7 -> combine(wf7Am, wf7Pm);
            case 8 -> wf8;
            case 9 -> wf9;
            case 10 -> wf10;
            default -> null;
        };
    }

    private static Integer max(Integer first, Integer second) {
        if (first == null) return second;
        if (second == null) return first;
        return Math.max(first, second);
    }

    private static String combine(String first, String second) {
        if (first == null) return second;
        if (second == null || first.equals(second)) return first;
        return first + " / " + second;
    }
}
