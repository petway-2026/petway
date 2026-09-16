/* AI 미설정, 입력·출력 검증 실패, 제공자 호출 실패를 각각 HTTP 오류 응답으로 변환한다. */
package com.petway.ai.recommendation;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RecommendationController.class)
public class RecommendationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RecommendationExceptionHandler.class);

    @ExceptionHandler(AiRecommendationUnavailableException.class)
    public ResponseEntity<?> handleUnavailable(AiRecommendationUnavailableException exception) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(InvalidRecommendationException.class)
    public ResponseEntity<?> handleInvalid(InvalidRecommendationException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(AiRecommendationException.class)
    public ResponseEntity<?> handleRecommendationFailure(AiRecommendationException exception) {
        // Keep the client response generic, but include the cause in server logs so
        // upstream HTTP errors (invalid key, quota, model access, etc.) are
        // diagnosable without exposing them to the browser.
        log.warn("AI recommendation failure: {}", exception.getMessage(), exception);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", exception.getMessage()));
    }
}
