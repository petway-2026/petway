/* 자동차·대중교통 경로 요청을 해당 제공자 클라이언트에 전달하고 경로 오류를 응답으로 바꾼다. */
package com.petway.route;

import com.petway.route.dto.RouteRequest;
import com.petway.route.dto.RouteResponse;
import com.petway.route.dto.TransitRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    private final KakaoDirectionsClient directionsClient;
    private final OdsayTransitClient transitClient;

    @PostMapping("/driving")
    public RouteResponse driving(@RequestBody RouteRequest request) {
        return directionsClient.findRoute(request);
    }

    @PostMapping("/transit")
    public TransitRouteResponse transit(@RequestBody RouteRequest request) {
        return transitClient.findRoute(request);
    }

    @ExceptionHandler(KakaoDirectionsClient.DirectionsException.class)
    public ResponseEntity<Map<String, String>> handleDirections(
            KakaoDirectionsClient.DirectionsException exception
    ) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(OdsayTransitClient.TransitException.class)
    public ResponseEntity<Map<String, String>> handleTransit(OdsayTransitClient.TransitException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", exception.getMessage()));
    }
}
