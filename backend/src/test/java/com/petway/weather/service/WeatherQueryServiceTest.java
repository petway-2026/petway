package com.petway.weather.service;

import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherForecast;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.port.WeatherForecastGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 28);
    private static final double LATITUDE = 37.5665;
    private static final double LONGITUDE = 126.9780;

    @Mock
    private WeatherForecastGateway gateway;

    private WeatherQueryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-28T03:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new WeatherQueryService(gateway, clock);
    }

    @Test
    void usesShortTermForTodayThroughDayThree() {
        LocalDate targetDate = TODAY.plusDays(3);
        WeatherForecast forecast = forecast(targetDate, ForecastType.SHORT_TERM);
        when(gateway.getShortTerm(
                LATITUDE,
                LONGITUDE,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).thenReturn(forecast);

        var response = service.getForecast(
                LATITUDE,
                LONGITUDE,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );

        assertThat(response.forecastType()).isEqualTo(ForecastType.SHORT_TERM);
        verify(gateway).getShortTerm(
                LATITUDE,
                LONGITUDE,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );
    }

    @Test
    void usesMidTermForDayFourThroughDayTen() {
        LocalDate targetDate = TODAY.plusDays(10);
        WeatherForecast forecast = forecast(targetDate, ForecastType.MID_TERM);
        when(gateway.getMidTerm(WeatherRegion.SEOUL_INCHEON_GYEONGGI, targetDate))
                .thenReturn(forecast);

        var response = service.getForecast(
                LATITUDE,
                LONGITUDE,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );

        assertThat(response.forecastType()).isEqualTo(ForecastType.MID_TERM);
        verify(gateway).getMidTerm(WeatherRegion.SEOUL_INCHEON_GYEONGGI, targetDate);
    }

    @Test
    void usesSeasonalOutlookForDayElevenThroughDayNinety() {
        LocalDate targetDate = TODAY.plusDays(90);
        WeatherForecast forecast = forecast(targetDate, ForecastType.SEASONAL_OUTLOOK);
        when(gateway.getSeasonalOutlook(WeatherRegion.JEJU, targetDate)).thenReturn(forecast);

        var response = service.getForecast(LATITUDE, LONGITUDE, targetDate, WeatherRegion.JEJU);

        assertThat(response.forecastType()).isEqualTo(ForecastType.SEASONAL_OUTLOOK);
        verify(gateway).getSeasonalOutlook(WeatherRegion.JEJU, targetDate);
    }

    @Test
    void requiresRegionForEveryForecastType() {
        assertThatThrownBy(() -> service.getForecast(
                LATITUDE,
                LONGITUDE,
                TODAY.plusDays(1),
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("날씨 조회에는 region이 필요합니다.");
        verifyNoInteractions(gateway);
    }

    @Test
    void rejectsPastAndBeyondNinetyDays() {
        assertThatThrownBy(() -> service.getForecast(
                LATITUDE, LONGITUDE, TODAY.minusDays(1), WeatherRegion.JEJU
        )).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getForecast(
                LATITUDE, LONGITUDE, TODAY.plusDays(91), WeatherRegion.JEJU
        )).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(gateway);
    }

    private WeatherForecast forecast(LocalDate date, ForecastType type) {
        return new WeatherForecast(
                date, type, null, null, null, null, null,
                null, null, null, null, null, "테스트"
        );
    }
}
