package com.petway.weather.service;

import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherForecastResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 외부 호출 중복을 줄이기 위한 프로세스 로컬 임시 캐시다.
 * 영속 저장소나 기상정보 원본으로 사용하지 않는다.
 */
@Component
public class WeatherForecastCache {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int MAX_CACHE_ENTRIES = 512;

    private final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;
    private final int maxEntries;

    @Autowired
    public WeatherForecastCache(Clock weatherClock) {
        this(weatherClock, CACHE_TTL, MAX_CACHE_ENTRIES);
    }

    WeatherForecastCache(Clock weatherClock, Duration ttl, int maxEntries) {
        this.clock = weatherClock;
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("캐시 TTL은 0보다 커야 합니다.");
        }
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("최대 캐시 항목 수는 0보다 커야 합니다.");
        }
        this.ttl = ttl;
        this.maxEntries = maxEntries;
    }

    public Optional<WeatherForecastResponse> get(
            double latitude,
            double longitude,
            LocalDate targetDate,
            WeatherRegion region,
            ForecastType forecastType
    ) {
        Key key = new Key(latitude, longitude, targetDate, region, forecastType);
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    public synchronized void put(
            double latitude,
            double longitude,
            LocalDate targetDate,
            WeatherRegion region,
            ForecastType forecastType,
            WeatherForecastResponse response
    ) {
        Instant now = clock.instant();
        removeExpired(now);
        Key key = new Key(latitude, longitude, targetDate, region, forecastType);
        if (!entries.containsKey(key) && entries.size() >= maxEntries) {
            entries.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().cachedAt()))
                    .ifPresent(entry -> entries.remove(entry.getKey(), entry.getValue()));
        }
        entries.put(key, new Entry(response, now, now.plus(ttl)));
    }

    private void removeExpired(Instant now) {
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record Key(
            double latitude,
            double longitude,
            LocalDate targetDate,
            WeatherRegion region,
            ForecastType forecastType
    ) {
    }

    private record Entry(
            WeatherForecastResponse response,
            Instant cachedAt,
            Instant expiresAt
    ) {
    }
}
