package com.petway.external.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KmaApiResponse<T>(Response<T> response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response<T>(Header header, Body<T> body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body<T>(Items<T> items, Integer totalCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items<T>(List<T> item) {
    }
}
