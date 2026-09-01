package com.petway.external.tour.client;

import com.petway.place.model.PlacePetDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Process-local cache for successful detailPetTour2 results. */
@Component
public class TourPetDetailCache {

    static final Duration DEFAULT_TTL = Duration.ofMinutes(15);
    static final int DEFAULT_MAX_ENTRIES = 512;

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;
    private final int maxEntries;

    @Autowired
    public TourPetDetailCache(Clock clock) {
        this(clock, DEFAULT_TTL, DEFAULT_MAX_ENTRIES);
    }

    TourPetDetailCache(Clock clock, Duration ttl, int maxEntries) {
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

    public Optional<PlacePetDetail> get(String contentId) {
        Entry entry = entries.get(contentId);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.expiresAt().isAfter(clock.instant())) {
            entries.remove(contentId, entry);
            return Optional.empty();
        }
        return Optional.of(entry.detail());
    }

    public synchronized void put(String contentId, PlacePetDetail detail) {
        Instant now = clock.instant();
        removeExpired(now);
        if (!entries.containsKey(contentId) && entries.size() >= maxEntries) {
            entries.entrySet().stream()
                    .min(Comparator.comparing(entry -> entry.getValue().cachedAt()))
                    .ifPresent(entry -> entries.remove(entry.getKey(), entry.getValue()));
        }
        entries.put(contentId, new Entry(detail, now, now.plus(ttl)));
    }

    private void removeExpired(Instant now) {
        entries.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record Entry(PlacePetDetail detail, Instant cachedAt, Instant expiresAt) {
    }
}
