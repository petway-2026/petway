package com.petway.weather.service;

import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherDisplay;
import com.petway.weather.dto.WeatherForecastResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherForecastCacheTest {

    @Test
    void expiresEntryAfterConfiguredTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-28T03:00:00Z"));
        WeatherForecastCache cache = new WeatherForecastCache(clock);
        LocalDate targetDate = LocalDate.of(2026, 8, 29);

        cache.put(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM,
                response(targetDate)
        );

        assertThat(cache.get(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM
        )).isPresent();

        clock.advance(Duration.ofMinutes(10));

        assertThat(cache.get(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM
        )).isEmpty();
    }

    @Test
    void evictsOldestEntryWhenMaximumSizeIsReached() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-28T03:00:00Z"));
        WeatherForecastCache cache = new WeatherForecastCache(clock, Duration.ofMinutes(10), 1);
        LocalDate firstDate = LocalDate.of(2026, 8, 29);
        LocalDate secondDate = LocalDate.of(2026, 8, 30);

        cache.put(
                37.5665, 126.9780, firstDate, WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM, response(firstDate)
        );
        clock.advance(Duration.ofSeconds(1));
        cache.put(
                37.5665, 126.9780, secondDate, WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM, response(secondDate)
        );

        assertThat(cache.get(
                37.5665, 126.9780, firstDate, WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM
        )).isEmpty();
        assertThat(cache.get(
                37.5665, 126.9780, secondDate, WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM
        )).isPresent();
    }

    @Test
    void separatesForecastTypesForSamePlaceAndTargetDate() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-28T03:00:00Z"));
        WeatherForecastCache cache = new WeatherForecastCache(clock);
        LocalDate targetDate = LocalDate.of(2026, 9, 1);

        cache.put(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.MID_TERM,
                response(targetDate, ForecastType.MID_TERM)
        );

        assertThat(cache.get(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.MID_TERM
        )).isPresent();
        assertThat(cache.get(
                37.5665,
                126.9780,
                targetDate,
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                ForecastType.SHORT_TERM
        )).isEmpty();
    }

    private WeatherForecastResponse response(LocalDate targetDate) {
        return response(targetDate, ForecastType.SHORT_TERM);
    }

    private WeatherForecastResponse response(LocalDate targetDate, ForecastType forecastType) {
        return new WeatherForecastResponse(
                targetDate,
                forecastType,
                "서울ㆍ인천ㆍ경기도",
                20.0,
                28.0,
                30,
                null,
                new WeatherDisplay("20~28℃", "강수확률 30%", "맑음", "테스트"),
                null,
                null,
                null,
                null,
                null,
                "테스트",
                "기상청"
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Asia/Seoul");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
