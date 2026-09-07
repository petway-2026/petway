
package com.petway.external.tour.client;

import com.petway.place.model.NearbyPlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Short-lived process-local cache for successful locationBasedList2 responses. */
@Component
public class TourNearbySearchCache {

    static final Duration DEFAULT_TTL = Duration.ofMinutes(2);
    static final int DEFAULT_MAX_ENTRIES = 256;

    private final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;
    private final int maxEntries;

    @Autowired
    public TourNearbySearchCache(Clock clock) {
        this(clock, DEFAULT_TTL, DEFAULT_MAX_ENTRIES);
    }

    TourNearbySearchCache(Clock clock, Duration ttl, int maxEntries) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Cache TTL must be positive.");
        }
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("Maximum cache entries must be positive.");
        }
        this.clock = clock;
        this.ttl = ttl;
        this.maxEntries = maxEntries;
    }

    public Optional<PlaceSearchResult> get(NearbyPlaceSearchCondition condition) {
        Key key = key(condition);
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.result());
    }

    public synchronized void put(NearbyPlaceSearchCondition condition, PlaceSearchResult result) {
        Instant now = clock.instant();
        removeExpired(now);
        Key key = key(condition);
        if (!entries.containsKey(key) && entries.size() >= maxEntries) {
            entries.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().cachedAt()))
                    .ifPresent(entry -> entries.remove(entry.getKey(), entry.getValue()));
        }
        entries.put(key, new Entry(result, now, now.plus(ttl)));
    }

    private Key key(NearbyPlaceSearchCondition condition) {
        return new Key(
                condition.latitude().stripTrailingZeros(),
                condition.longitude().stripTrailingZeros(),
                condition.radius(),
                condition.page(),
                condition.size()
        );
    }

    private void removeExpired(Instant now) {
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record Key(BigDecimal latitude, BigDecimal longitude, int radius, int page, int size) {
    }

    private record Entry(PlaceSearchResult result, Instant cachedAt, Instant expiresAt) {
    }
}
