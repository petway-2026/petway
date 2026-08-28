package com.petway.weather.controller;

import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherForecastResponse;
import com.petway.weather.service.WeatherQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherQueryService weatherQueryService;

    @Operation(
            summary = "여행일 날씨 조회",
            description = "D0~D3은 단기예보, D4~D10은 중기예보, D11~D90은 월별 3개월 전망을 반환합니다. 모든 조회에 region이 필요합니다."
    )
    @GetMapping
    public ResponseEntity<WeatherForecastResponse> getWeather(
            @Parameter(example = "37.5665")
            @RequestParam double latitude,
            @Parameter(example = "126.9780")
            @RequestParam double longitude,
            @Parameter(example = "2026-09-05")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate targetDate,
            @Parameter(description = "PetWay 예보 권역")
            @RequestParam
            WeatherRegion region
    ) {
        return ResponseEntity.ok(
                weatherQueryService.getForecast(latitude, longitude, targetDate, region)
        );
    }
}
