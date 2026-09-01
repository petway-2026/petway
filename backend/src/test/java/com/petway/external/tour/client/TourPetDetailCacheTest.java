package com.petway.external.tour.client;

import com.petway.place.model.PlacePetDetail;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TourPetDetailCacheTest {

    @Test
    void evictsOldestEntryWhenMaximumSizeIsReached() {
        TourPetDetailCache cache = new TourPetDetailCache(
                Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(15),
                1
        );
        PlacePetDetail first = new PlacePetDetail(null, null, "목줄", null, null);
        PlacePetDetail second = PlacePetDetail.empty();

        cache.put("100", first);
        cache.put("200", second);

        assertThat(cache.get("100")).isEmpty();
        assertThat(cache.get("200")).contains(second);
    }

    @Test
    void expiresEntryAfterTtl() {
        Instant cachedAt = Instant.parse("2026-09-01T00:00:00Z");
        MutableClock clock = new MutableClock(cachedAt);
        TourPetDetailCache cache = new TourPetDetailCache(
                clock,
                Duration.ofMinutes(15),
                1
        );
        cache.put("100", PlacePetDetail.empty());

        clock.advance(Duration.ofMinutes(16));

        assertThat(cache.get("100")).isEmpty();
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
