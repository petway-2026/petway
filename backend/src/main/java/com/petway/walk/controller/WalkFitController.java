package com.petway.walk.controller;

import com.petway.walk.dto.WalkFitRecommendationRequest;
import com.petway.walk.dto.WalkFitRecommendationResponse;
import com.petway.walk.service.WalkFitRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/walk")
@RequiredArgsConstructor
public class WalkFitController {

    private final WalkFitRecommendationService recommendationService;

    @GetMapping("/recommendations")
    public WalkFitRecommendationResponse getRecommendations(
            @Valid @ParameterObject @ModelAttribute WalkFitRecommendationRequest request,
            Authentication authentication
    ) {
        return recommendationService.recommend(
                authentication.getName(),
                request.getLatitude(),
                request.getLongitude(),
                request.getTargetDate()
        );
    }
}
