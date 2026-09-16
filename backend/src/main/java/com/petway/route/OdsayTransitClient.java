/* 인접 방문지 사이의 대중교통 경로를 조회하고 구간 캐시를 이용해 반복 호출을 줄인다. */
package com.petway.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.petway.route.dto.RouteRequest;
import com.petway.route.dto.TransitRouteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OdsayTransitClient {
    private static final String TRANSIT_URL = "https://api.odsay.com/v1/api/searchPubTransPathT";

    private final String apiKey;
    private final RestClient restClient;
    private final Duration cacheTtl;
    private final int cacheMaxEntries;
    private final ConcurrentHashMap<LegCacheKey, CachedLeg> legCache = new ConcurrentHashMap<>();

    public OdsayTransitClient(
            @Value("${ODSAY_API_KEY:}") String apiKey,
            @Value("${ODSAY_CACHE_MINUTES:360}") long cacheMinutes,
            @Value("${ODSAY_CACHE_MAX_ENTRIES:1000}") int cacheMaxEntries
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.cacheTtl = Duration.ofMinutes(Math.max(1, cacheMinutes));
        this.cacheMaxEntries = Math.max(10, cacheMaxEntries);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public TransitRouteResponse findRoute(RouteRequest request) {
        validate(request);
        if (apiKey.isBlank()) throw new TransitException("ODsay 대중교통 API 키가 설정되지 않았습니다.");

        List<TransitRouteResponse.TransitLeg> legs = new ArrayList<>();
        int totalDuration = 0;
        int totalFare = 0;
        for (int index = 0; index < request.stops().size() - 1; index++) {
            TransitRouteResponse.TransitLeg leg = findLeg(request.stops().get(index), request.stops().get(index + 1));
            legs.add(leg);
            totalDuration += leg.durationMinutes();
            totalFare += leg.fare();
        }
        return new TransitRouteResponse(totalDuration, totalFare, List.copyOf(legs));
    }

    private TransitRouteResponse.TransitLeg findLeg(RouteRequest.RouteStop from, RouteRequest.RouteStop to) {
        removeExpiredEntriesIfNeeded();
        LegCacheKey key = new LegCacheKey(from.latitude(), from.longitude(), to.latitude(), to.longitude());
        Instant now = Instant.now();
        CachedLeg cached = legCache.compute(key, (ignored, current) -> {
            if (current != null && current.expiresAt().isAfter(now)) return current;
            TransitRouteResponse.TransitLeg fetched = fetchLeg(from, to);
            return new CachedLeg(
                    fetched.durationMinutes(),
                    fetched.fare(),
                    fetched.transfers(),
                    fetched.steps(),
                    now.plus(cacheTtl)
            );
        });
        return new TransitRouteResponse.TransitLeg(
                from.id(),
                to.id(),
                cached.durationMinutes(),
                cached.fare(),
                cached.transfers(),
                cached.steps()
        );
    }

    private TransitRouteResponse.TransitLeg fetchLeg(RouteRequest.RouteStop from, RouteRequest.RouteStop to) {
        URI uri = UriComponentsBuilder.fromUriString(TRANSIT_URL)
                .queryParam("SX", from.longitude())
                .queryParam("SY", from.latitude())
                .queryParam("EX", to.longitude())
                .queryParam("EY", to.latitude())
                .queryParam("OPT", 0)
                .queryParam("apiKey", apiKey)
                .encode()
                .build()
                .toUri();
        try {
            JsonNode root = restClient.get().uri(uri).retrieve().body(JsonNode.class);
            JsonNode error = root == null ? null : root.path("error");
            if (error != null && !error.isMissingNode() && !error.isEmpty()) {
                JsonNode first = error.isArray() ? error.path(0) : error;
                throw new TransitException(first.path("message").asText(first.path("msg").asText("대중교통 경로를 찾지 못했습니다.")));
            }
            JsonNode paths = root == null ? null : root.path("result").path("path");
            if (paths == null || !paths.isArray() || paths.isEmpty()) {
                throw new TransitException(from.name() + "에서 " + to.name() + "까지 대중교통 경로가 없습니다.");
            }
            JsonNode best = null;
            int shortestTime = Integer.MAX_VALUE;
            for (JsonNode path : paths) {
                int totalTime = path.path("info").path("totalTime").asInt(Integer.MAX_VALUE);
                if (totalTime < shortestTime) {
                    shortestTime = totalTime;
                    best = path;
                }
            }
            if (best == null) throw new TransitException("추천할 대중교통 경로가 없습니다.");
            JsonNode info = best.path("info");
            List<TransitRouteResponse.TransitStep> steps = parseSteps(best.path("subPath"));
            int rideCount = info.path("busTransitCount").asInt() + info.path("subwayTransitCount").asInt();
            return new TransitRouteResponse.TransitLeg(
                    from.id(),
                    to.id(),
                    info.path("totalTime").asInt(),
                    info.path("payment").asInt(),
                    Math.max(0, rideCount - 1),
                    steps
            );
        } catch (TransitException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new TransitException("ODsay 대중교통 정보를 가져오지 못했습니다.", exception);
        }
    }

    private void removeExpiredEntriesIfNeeded() {
        if (legCache.size() < cacheMaxEntries) return;
        Instant now = Instant.now();
        legCache.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        if (legCache.size() >= cacheMaxEntries) legCache.clear();
    }

    private List<TransitRouteResponse.TransitStep> parseSteps(JsonNode subPaths) {
        List<TransitRouteResponse.TransitStep> steps = new ArrayList<>();
        if (!subPaths.isArray()) return steps;
        for (JsonNode step : subPaths) {
            int trafficType = step.path("trafficType").asInt();
            String type = switch (trafficType) {
                case 1 -> "SUBWAY";
                case 2 -> "BUS";
                default -> "WALK";
            };
            String start = step.path("startName").asText("");
            String end = step.path("endName").asText("");
            String line = lineName(step.path("lane"));
            String description = type.equals("WALK")
                    ? "도보 이동"
                    : String.join(" · ", List.of(line, start + " → " + end).stream().filter(value -> !value.isBlank() && !value.equals(" → ")).toList());
            steps.add(new TransitRouteResponse.TransitStep(
                    type,
                    step.path("sectionTime").asInt(),
                    step.path("distance").asInt(),
                    description
            ));
        }
        return List.copyOf(steps);
    }

    private String lineName(JsonNode lanes) {
        if (!lanes.isArray() || lanes.isEmpty()) return "";
        JsonNode lane = lanes.path(0);
        return lane.path("busNo").asText(lane.path("name").asText("")).trim();
    }

    private void validate(RouteRequest request) {
        if (request == null || request.stops().size() < 2 || request.stops().size() > 7) {
            throw new IllegalArgumentException("대중교통 길찾기는 2곳 이상 7곳 이하의 장소가 필요합니다.");
        }
    }

    private record LegCacheKey(
            double fromLatitude,
            double fromLongitude,
            double toLatitude,
            double toLongitude
    ) {
    }

    private record CachedLeg(
            int durationMinutes,
            int fare,
            int transfers,
            List<TransitRouteResponse.TransitStep> steps,
            Instant expiresAt
    ) {
        private CachedLeg {
            steps = List.copyOf(steps);
        }
    }

    public static class TransitException extends RuntimeException {
        public TransitException(String message) { super(message); }
        public TransitException(String message, Throwable cause) { super(message, cause); }
    }
}
