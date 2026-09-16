/* 관광공사 실제 API와 통합 검색 페이지를 비교하는 선택 실행 검증이다. 외부 호출이 발생한다. */
package com.petway.place.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petway.external.tour.client.*;
import com.petway.external.tour.config.*;
import com.petway.external.tour.mapper.TourPlaceMapper;
import com.petway.facility.PetFacilityCatalog;
import com.petway.place.dto.NearbyPlaceSearchRequest;
import com.petway.place.dto.PlaceSummaryResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicitly invoked live check; never runs during the normal offline test suite. */
public class NearbyPaginationLiveCheck {
    public static void main(String[] args) {
        var calls = new AtomicInteger();
        try {
            String key = System.getenv("TOUR_API_KEY");
            require(key != null && !key.isBlank(), "TOUR_API_KEY is required");
            var properties = new TourApiProperties("https://apis.data.go.kr/B551011/KorPetTourService2",
                    key, "ETC", "PetWay", 100, Duration.ofSeconds(5), Duration.ofSeconds(15));
            var builder = RestClient.builder().requestInterceptor((request, body, execution) -> {
                require(calls.incrementAndGet() <= 15, "Live API budget exceeded");
                return execution.execute(request, body);
            });
            Clock clock = Clock.systemUTC();
            var gateway = new TourApiClient(new TourApiConfig().tourApiRestClient(builder, properties), properties,
                    new TourPlaceMapper(new ObjectMapper()), new TourPetDetailCache(clock), new TourNearbySearchCache(clock));
            var catalog = new PetFacilityCatalog();
            ReflectionTestUtils.invokeMethod(catalog, "load");
            var service = new PlaceQueryService(gateway, catalog, new PlaceSearchSnapshotCache(clock));
            Set<String> previous = Set.of();
            for (int radius : new int[]{5000, 10000, 20000, 30000}) {
                var request = new NearbyPlaceSearchRequest();
                request.setLatitude(new BigDecimal("37.5665"));
                request.setLongitude(new BigDecimal("126.9780"));
                request.setRadius(radius);
                request.setSize(100);
                var first = service.searchNearby(request);
                int callsAfterSnapshot = calls.get();
                var ids = new HashSet<String>();
                for (int page = 1; page <= Math.max(1, (first.totalCount() + 99) / 100); page++) {
                    request.setPage(page);
                    var result = service.searchNearby(request);
                    require(result.totalCount() == first.totalCount(), "Total count changed across pages");
                    require(result.items().size() <= 100, "Oversized page");
                    for (PlaceSummaryResponse place : result.items()) {
                        require(ids.add(place.contentId()), "Duplicate content ID across pages");
                    }
                }
                require(ids.size() == first.totalCount(), "Total count does not equal unique results");
                require(calls.get() == callsAfterSnapshot, "Page request repeated provider lookup");
                require(ids.containsAll(previous), "Larger radius lost earlier results");
                System.out.printf("radius=%d total=%d unique=%d duplicates=0 previousIncluded=true calls=%d%n",
                        radius, first.totalCount(), ids.size(), calls.get());
                previous = ids;
            }
            System.out.println("PASS live nearby pagination; KTO calls=" + calls.get());
        } catch (Exception exception) {
            // Never print request URLs or exception chains containing a service key.
            System.err.println("FAIL live nearby pagination: " + exception.getClass().getSimpleName() + "; KTO calls=" + calls.get());
            System.exit(1);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
