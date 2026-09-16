/* PlaceSearchSnapshotCache의 동작을 검증하는 테스트다. 검증 대상: 완성된 통합 검색 목록을 페이지 간 공유하고 동일 조건의 동시 로딩을 하나로 합친다. */
package com.petway.place.service;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class PlaceSearchSnapshotCacheTest {
    @Test
    void reusesCompleteResultsUntilExpiry() {
        var now = new AtomicReference<>(Instant.parse("2026-09-08T00:00:00Z"));
        Clock clock = org.mockito.Mockito.mock(Clock.class);
        org.mockito.Mockito.when(clock.instant()).thenAnswer(ignored -> now.get());
        var cache = new PlaceSearchSnapshotCache(clock);
        var loads = new AtomicInteger();
        java.util.function.Supplier<List<com.petway.place.dto.PlaceSummaryResponse>> loader = () -> {
            loads.incrementAndGet();
            return List.of();
        };
        cache.get("query", loader);
        cache.get("query", loader);
        assertThat(loads.get()).isEqualTo(1);
        now.set(now.get().plusSeconds(121));
        cache.get("query", loader);
        assertThat(loads.get()).isEqualTo(2);
    }

    @Test
    void coalescesConcurrentPagesWithoutCachingProviderFailures() throws Exception {
        var cache = new PlaceSearchSnapshotCache(Clock.systemUTC());
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var loads = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> cache.get("query", () -> {
                loads.incrementAndGet();
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return List.of();
            }));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            var second = pool.submit(() -> cache.get("query", () -> {
                loads.incrementAndGet();
                return List.of();
            }));
            release.countDown();
            assertThat(first.get(5, TimeUnit.SECONDS)).isEmpty();
            assertThat(second.get(5, TimeUnit.SECONDS)).isEmpty();
            assertThat(loads.get()).isEqualTo(1);
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
        assertThatThrownBy(() -> cache.get("failed", () -> { throw new IllegalStateException("provider failed"); }))
                .hasMessage("provider failed");
        assertThat(cache.get("failed", List::of)).isEmpty();
    }

    @Test
    void boundsStoredSnapshots() {
        var cache = new PlaceSearchSnapshotCache(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int key = 0; key < 17; key++) cache.get(key, List::of);
        var loads = new AtomicInteger();
        cache.get(0, () -> { loads.incrementAndGet(); return List.of(); });
        assertThat(loads.get()).isEqualTo(1);
    }
}
