package com.petway.external.weather.mapper;

import com.petway.external.weather.dto.KmaShortForecastItem;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.domain.WeatherRegion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KmaWeatherMapperTest {

    private final KmaWeatherMapper mapper = new KmaWeatherMapper();

    @Test
    void mapsHourlyFixtureToDailySummary() {
        List<KmaShortForecastItem> fixture = List.of(
                item("TMP", "0900", "21"),
                item("TMP", "1500", "28"),
                item("POP", "0900", "20"),
                item("POP", "1500", "70"),
                item("SKY", "0900", "3"),
                item("PTY", "1500", "1")
        );

        var forecast = mapper.mapShortTerm(
                fixture,
                LocalDate.of(2026, 9, 1),
                LocalDateTime.of(2026, 8, 31, 23, 0),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );

        assertThat(forecast.forecastType()).isEqualTo(ForecastType.SHORT_TERM);
        assertThat(forecast.minimumTemperature()).isEqualTo(21.0);
        assertThat(forecast.maximumTemperature()).isEqualTo(28.0);
        assertThat(forecast.rainProbability()).isEqualTo(70);
        assertThat(forecast.weatherCondition()).isEqualTo(WeatherCondition.RAIN);
        assertThat(forecast.regionName()).isEqualTo("서울ㆍ인천ㆍ경기도");
    }

    @Test
    void rejectsIncompleteShortTermDailySummary() {
        List<KmaShortForecastItem> fixture = List.of(
                item("TMP", "0900", "21"),
                item("TMP", "1500", "28"),
                item("SKY", "1500", "1")
        );

        assertThatThrownBy(() -> mapper.mapShortTerm(
                fixture,
                LocalDate.of(2026, 9, 1),
                LocalDateTime.of(2026, 8, 31, 23, 0),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("누락");
    }

    @Test
    void combinesRainAndSnowAcrossHourlyForecasts() {
        List<KmaShortForecastItem> fixture = List.of(
                item("TMP", "0900", "1"),
                item("TMP", "1500", "5"),
                item("POP", "0900", "70"),
                item("SKY", "0900", "4"),
                item("PTY", "0900", "1"),
                item("PTY", "1500", "3")
        );

        var forecast = mapper.mapShortTerm(
                fixture,
                LocalDate.of(2026, 9, 1),
                LocalDateTime.of(2026, 8, 31, 23, 0),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );

        assertThat(forecast.weatherCondition()).isEqualTo(WeatherCondition.RAIN_OR_SNOW);
    }

    @Test
    void rejectsInvalidDailyForecastRanges() {
        List<KmaShortForecastItem> fixture = List.of(
                item("TMN", "0600", "29"),
                item("TMX", "1500", "20"),
                item("POP", "1500", "120"),
                item("SKY", "1500", "1")
        );

        assertThatThrownBy(() -> mapper.mapShortTerm(
                fixture,
                LocalDate.of(2026, 9, 1),
                LocalDateTime.of(2026, 8, 31, 23, 0),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("범위");
    }

    private KmaShortForecastItem item(String category, String time, String value) {
        return new KmaShortForecastItem(
                "20260831", "2300", category, "20260901", time, value, 60, 127
        );
    }
}
