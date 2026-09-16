/* 방문 순서에 따른 자동차 경로를 카카오 모빌리티에서 받아 구간 거리·시간·선 좌표를 구성한다. */
package com.petway.route;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.petway.route.dto.RouteRequest;
import com.petway.route.dto.RouteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class KakaoDirectionsClient {
    private static final String DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";

    private final String apiKey;
    private final RestClient restClient;

    public KakaoDirectionsClient(@Value("${KAKAO_REST_API_KEY:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public RouteResponse findRoute(RouteRequest request) {
        validate(request);
        if (apiKey.isBlank()) {
            throw new DirectionsException("카카오 길찾기 API 키가 설정되지 않았습니다.");
        }

        List<RouteRequest.RouteStop> stops = request.stops();
        URI uri = createUri(stops);
        try {
            KakaoDirectionsResponse response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoDirectionsResponse.class);
            return mapResponse(stops, response);
        } catch (DirectionsException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new DirectionsException("카카오 길찾기 정보를 가져오지 못했습니다.", exception);
        }
    }

    private URI createUri(List<RouteRequest.RouteStop> stops) {
        RouteRequest.RouteStop origin = stops.get(0);
        RouteRequest.RouteStop destination = stops.get(stops.size() - 1);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(DIRECTIONS_URL)
                .queryParam("origin", coordinate(origin))
                .queryParam("destination", coordinate(destination))
                .queryParam("priority", "RECOMMEND")
                .queryParam("summary", false)
                .queryParam("alternatives", false)
                .queryParam("road_details", false);
        if (stops.size() > 2) {
            String waypoints = stops.subList(1, stops.size() - 1).stream()
                    .map(this::coordinate)
                    .collect(Collectors.joining("|"));
            builder.queryParam("waypoints", waypoints);
        }
        return builder.encode().build().toUri();
    }

    private String coordinate(RouteRequest.RouteStop stop) {
        return stop.longitude() + "," + stop.latitude();
    }

    private RouteResponse mapResponse(
            List<RouteRequest.RouteStop> stops,
            KakaoDirectionsResponse response
    ) {
        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new DirectionsException("카카오 길찾기 결과가 비어 있습니다.");
        }
        Route route = response.routes().get(0);
        if (route.resultCode() != 0 || route.summary() == null || route.sections() == null) {
            throw new DirectionsException(route.resultMsg() == null ? "경로를 찾지 못했습니다." : route.resultMsg());
        }
        if (route.sections().size() != stops.size() - 1) {
            throw new DirectionsException("길찾기 구간 수가 요청한 장소 수와 일치하지 않습니다.");
        }

        List<RouteResponse.RouteLeg> legs = new ArrayList<>();
        List<RouteResponse.RoutePoint> path = new ArrayList<>();
        for (int sectionIndex = 0; sectionIndex < route.sections().size(); sectionIndex++) {
            Section section = route.sections().get(sectionIndex);
            legs.add(new RouteResponse.RouteLeg(
                    stops.get(sectionIndex).id(),
                    stops.get(sectionIndex + 1).id(),
                    section.distance(),
                    section.duration()
            ));
            if (section.roads() == null) continue;
            for (Road road : section.roads()) {
                List<Double> vertexes = road.vertexes();
                if (vertexes == null) continue;
                for (int index = 0; index + 1 < vertexes.size(); index += 2) {
                    path.add(new RouteResponse.RoutePoint(vertexes.get(index + 1), vertexes.get(index)));
                }
            }
        }
        return new RouteResponse(route.summary().distance(), route.summary().duration(), legs, path);
    }

    private void validate(RouteRequest request) {
        if (request == null || request.stops().size() < 2 || request.stops().size() > 7) {
            throw new IllegalArgumentException("길찾기는 2곳 이상 7곳 이하의 장소가 필요합니다.");
        }
        for (RouteRequest.RouteStop stop : request.stops()) {
            if (stop == null || stop.id() == null || stop.id().isBlank()
                    || !Double.isFinite(stop.latitude()) || stop.latitude() < -90 || stop.latitude() > 90
                    || !Double.isFinite(stop.longitude()) || stop.longitude() < -180 || stop.longitude() > 180) {
                throw new IllegalArgumentException("길찾기 장소 정보가 올바르지 않습니다.");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoDirectionsResponse(List<Route> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Route(
            @JsonProperty("result_code") int resultCode,
            @JsonProperty("result_msg") String resultMsg,
            Summary summary,
            List<Section> sections
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Summary(int distance, int duration) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Section(int distance, int duration, List<Road> roads) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Road(List<Double> vertexes) {
    }

    public static class DirectionsException extends RuntimeException {
        public DirectionsException(String message) {
            super(message);
        }

        public DirectionsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
