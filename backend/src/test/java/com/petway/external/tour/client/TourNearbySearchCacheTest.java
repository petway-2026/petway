package com.petway.external.tour.client;

import com.petway.place.model.NearbyPlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourNearbySearchCacheTest {

    private static final PlaceSearchResult RESULT = new PlaceSearchResult(1, 100, 0, List.of());

    @Test
    void reusesMinorCoordinateMovementButKeepsRequestParametersDistinct() {
        TourNearbySearchCache cache = new TourNearbySearchCache(
                Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(2),
                10
        );
        cache.put(condition("37.56651", "126.97801", 5000, 1, 100), RESULT);

        assertThat(cache.get(condition("37.56654", "126.97804", 5000, 1, 100)))
                .contains(RESULT);
        assertThat(cache.get(condition("37.56654", "126.97804", 5001, 1, 100)))
                .isEmpty();
        assertThat(cache.get(condition("37.56654", "126.97804", 5000, 2, 100)))
                .isEmpty();
    }

    @Test
    void expiresEntryAfterTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-01T00:00:00Z"));
        TourNearbySearchCache cache = new TourNearbySearchCache(clock, Duration.ofMinutes(2), 10);
        NearbyPlaceSearchCondition condition = condition("37.5665", "126.9780", 5000, 1, 100);
        cache.put(condition, RESULT);

        clock.advance(Duration.ofMinutes(3));

        assertThat(cache.get(condition)).isEmpty();
    }

    @Test
    void evictsOldestEntryWhenMaximumSizeIsReached() {
        TourNearbySearchCache cache = new TourNearbySearchCache(
                Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(2),
                1
        );
        NearbyPlaceSearchCondition first = condition("37.5665", "126.9780", 5000, 1, 100);
        NearbyPlaceSearchCondition second = condition("37.5665", "126.9780", 5000, 2, 100);

        cache.put(first, RESULT);
        cache.put(second, RESULT);

        assertThat(cache.get(first)).isEmpty();
        assertThat(cache.get(second)).contains(RESULT);
    }

    private NearbyPlaceSearchCondition condition(
            String latitude,
            String longitude,
            int radius,
            int page,
            int size
    ) {
        return new NearbyPlaceSearchCondition(
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                radius,
                page,
                size
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
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
