/* 여행 코스 추천 요청을 AI 추천 포트로 전달하는 REST API다. */
package com.petway.ai.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final AiRecommendationPort recommendationPort;

    @PostMapping("/travel-course")
    public RecommendationResult recommendTravelCourse(@RequestBody RecommendationInput input) {
        return recommendationPort.recommend(input);
    }
}
