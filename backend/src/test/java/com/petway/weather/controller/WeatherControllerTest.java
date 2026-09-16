/* WeatherController의 동작을 검증하는 테스트다. 검증 대상: 장소 좌표·방문일·예보 권역을 받아 날씨 조회 서비스로 전달한다. */
package com.petway.weather.controller;

import com.petway.global.exception.GlobalExceptionHandler;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherForecastResponse;
import com.petway.weather.dto.WeatherDisplay;
import com.petway.weather.service.WeatherForecastUnavailableException;
import com.petway.weather.service.WeatherProviderException;
import com.petway.weather.service.WeatherProviderTimeoutException;
import com.petway.weather.service.WeatherQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.SocketTimeoutException;
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
                new WeatherDisplay("21~28℃", "강수확률 40%", "흐림", "흐린 날씨가 예상되며, 강수확률 40%입니다."),
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
                .andExpect(jsonPath("$.display.temperature").value("21~28℃"))
                .andExpect(jsonPath("$.temperatureOutlook").doesNotExist())
                .andExpect(jsonPath("$.source").value("기상청"));

        verify(service).getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );
    }

    @Test
    void returnsNotFoundWhenForecastForBoundaryDateIsNotPublished() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 9, 7);
        when(service.getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).thenThrow(new WeatherForecastUnavailableException(
                "요청한 여행일(2026-09-07)의 중기예보가 아직 제공되지 않았습니다."
        ));

        mockMvc.perform(get("/api/weather")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-09-07")
                        .param("region", "SEOUL_INCHEON_GYEONGGI"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "요청한 여행일(2026-09-07)의 중기예보가 아직 제공되지 않았습니다."
                ));
    }

    @Test
    void returnsGatewayTimeoutWhenWeatherProviderTimesOut() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 29);
        when(service.getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).thenThrow(new WeatherProviderTimeoutException(
                "기상청 날씨 정보 호출 시간이 초과되었습니다.",
                new SocketTimeoutException("Read timed out")
        ));

        mockMvc.perform(get("/api/weather")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-08-29")
                        .param("region", "SEOUL_INCHEON_GYEONGGI"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.message").value("기상청 날씨 정보 응답 시간이 초과되었습니다."));
    }

    @Test
    void returnsBadGatewayWhenWeatherProviderFails() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 29);
        when(service.getForecast(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).thenThrow(new WeatherProviderException("기상청 응답 형식 오류"));

        mockMvc.perform(get("/api/weather")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-08-29")
                        .param("region", "SEOUL_INCHEON_GYEONGGI"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("기상청 날씨 정보 호출에 실패했습니다."));
    }
}
