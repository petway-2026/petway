package com.petway.place.service;

import com.petway.place.dto.PlaceSummaryResponse;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/** Reuses a complete merged result across pages and coalesces concurrent loads. */
@Component
public class PlaceSearchSnapshotCache {
    private static final Duration TTL = Duration.ofMinutes(2);
    private static final int MAX_ENTRIES = 16;
    private final Clock clock;
    private final Semaphore loadPermits = new Semaphore(4);
    private final LinkedHashMap<Object, Snapshot> snapshots = new LinkedHashMap<>();
    private final ConcurrentHashMap<Object, CompletableFuture<List<PlaceSummaryResponse>>> loading =
            new ConcurrentHashMap<>();

    public PlaceSearchSnapshotCache(Clock clock) {
        this.clock = clock;
    }

    public List<PlaceSummaryResponse> get(Object key, Supplier<List<PlaceSummaryResponse>> loader) {
        List<PlaceSummaryResponse> cached = cached(key);
        if (cached != null) return cached;
        var future = new CompletableFuture<List<PlaceSummaryResponse>>();
        var existing = loading.putIfAbsent(key, future);
        if (existing != null) {
            try {
                return existing.join();
            } catch (CompletionException exception) {
                if (exception.getCause() instanceof RuntimeException cause) throw cause;
                throw exception;
            }
        }
        boolean acquired = false;
        try {
            // Another load may have completed between the first lookup and putIfAbsent.
            cached = cached(key);
            if (cached == null) {
                acquired = loadPermits.tryAcquire();
                if (!acquired) throw new com.petway.external.tour.exception.TourApiException(
                        com.petway.external.tour.exception.TourApiErrorType.RATE_LIMITED,
                        "장소 검색 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
            }
            List<PlaceSummaryResponse> result = cached != null ? cached : List.copyOf(loader.get());
            if (cached == null) put(key, result);
            future.complete(result);
            return result;
        } catch (RuntimeException | Error exception) {
            future.completeExceptionally(exception);
            throw exception;
        } finally {
            if (acquired) loadPermits.release();
            loading.remove(key, future);
        }
    }

    private synchronized List<PlaceSummaryResponse> cached(Object key) {
        Snapshot snapshot = snapshots.get(key);
        if (snapshot == null) return null;
        if (!snapshot.expiresAt().isAfter(clock.instant())) {
            snapshots.remove(key);
            return null;
        }
        return snapshot.items();
    }

    private synchronized void put(Object key, List<PlaceSummaryResponse> items) {
        Instant now = clock.instant();
        snapshots.values().removeIf(snapshot -> !snapshot.expiresAt().isAfter(now));
        if (snapshots.size() >= MAX_ENTRIES) snapshots.remove(snapshots.keySet().iterator().next());
        snapshots.put(key, new Snapshot(items, now.plus(TTL)));
    }

    private record Snapshot(List<PlaceSummaryResponse> items, Instant expiresAt) { }
}
