package com.petway.weather.controller;

import com.petway.global.exception.GlobalExceptionHandler;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherForecastResponse;
import com.petway.weather.service.WeatherQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WeatherControllerTest {

    private WeatherQueryService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(WeatherQueryService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WeatherController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void bindsCoordinatesDateAndPetWayRegion() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 9, 5);
        WeatherForecastResponse response = new WeatherForecastResponse(
                targetDate,
                ForecastType.MID_TERM,
                "서울ㆍ인천ㆍ경기도",
                21.0,
                28.0,
                40,
                null,
                null,
                null,
                null,
                null,
                null,
                "중기예보",
                "기상청"
        );
        when(service.getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).thenReturn(response);

        mockMvc.perform(get("/api/weather")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-09-05")
                        .param("region", "SEOUL_INCHEON_GYEONGGI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecastType").value("MID_TERM"))
                .andExpect(jsonPath("$.source").value("기상청"));

        verify(service).getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );
    }
}
