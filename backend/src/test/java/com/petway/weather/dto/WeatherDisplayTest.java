package com.petway.weather.dto;

import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.OutlookLevel;
import com.petway.weather.domain.OutlookProbabilities;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.domain.WeatherForecast;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherDisplayTest {

    @Test
    void createsDailyDisplayFromShortTermValues() {
        WeatherForecast forecast = new WeatherForecast(
                LocalDate.of(2026, 8, 29),
                ForecastType.SHORT_TERM,
                "서울ㆍ인천ㆍ경기도",
                24.0,
                30.0,
                60,
                WeatherCondition.RAIN,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 8, 28, 11, 0),
                "시간별 예보 요약"
        );

        WeatherDisplay display = WeatherDisplay.from(forecast);

        assertThat(display.temperature()).isEqualTo("24~30℃");
        assertThat(display.precipitation()).isEqualTo("강수확률 60%");
        assertThat(display.weather()).isEqualTo("비");
        assertThat(display.summary()).isEqualTo("비가 예상되며, 강수확률 60%입니다.");
    }

    @Test
    void createsSeasonalDisplayFromMonthlyProbabilities() {
        WeatherForecast forecast = new WeatherForecast(
                LocalDate.of(2026, 11, 15),
                ForecastType.SEASONAL_OUTLOOK,
                "서울ㆍ인천ㆍ경기도",
                null,
                null,
                null,
                null,
                OutlookLevel.ABOVE_NORMAL,
                OutlookLevel.NEAR_NORMAL,
                new OutlookProbabilities(20, 30, 50),
                new OutlookProbabilities(20, 50, 30),
                LocalDateTime.of(2026, 8, 24, 10, 0),
                "월별 전망"
        );

        WeatherDisplay display = WeatherDisplay.from(forecast);

        assertThat(display.temperature()).isEqualTo("평년보다 높을 가능성 50%");
        assertThat(display.precipitation()).isEqualTo("평년과 비슷할 가능성 50%");
        assertThat(display.weather()).isEqualTo("월별 기후 전망");
        assertThat(display.summary())
                .isEqualTo("기온은 평년보다 높을 가능성 50%이며, 강수량은 평년과 비슷할 가능성 50%입니다.");
    }
}
