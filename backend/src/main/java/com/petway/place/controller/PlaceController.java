/* 장소 검색, 동반 원문 조회, 로그인 반려견 기준 동반 가능 판정 API를 제공한다. */
package com.petway.place.controller;

import com.petway.place.dto.PlaceSearchRequest;
import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.dto.NearbyPlaceSearchRequest;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.service.PlaceCompatibilityService;
import com.petway.place.service.PlaceQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceQueryService placeQueryService;
    private final PlaceCompatibilityService placeCompatibilityService;

    @GetMapping
    public PlaceSearchResponse searchPlaces(
            @Valid @ParameterObject @ModelAttribute PlaceSearchRequest request
    ) {
        return placeQueryService.searchByArea(request);
    }

    @GetMapping("/nearby")
    public PlaceSearchResponse searchNearby(
            @Valid @ParameterObject @ModelAttribute NearbyPlaceSearchRequest request
    ) {
        return placeQueryService.searchNearby(request);
    }

    @GetMapping("/{contentId}/pet-detail")
    public PlacePetDetailResponse getPetDetail(@PathVariable String contentId) {
        validateContentId(contentId);
        return placeQueryService.getPetDetail(contentId);
    }

    @GetMapping("/{contentId}/compatibility")
    public PlaceCompatibilityResponse getCompatibility(
            @PathVariable String contentId,
            Authentication authentication
    ) {
        validateContentId(contentId);
        return placeCompatibilityService.getCompatibility(
                authentication.getName(),
                contentId
        );
    }

    private void validateContentId(String contentId) {
        if (!contentId.matches("(?:\\d+|csv-[0-9a-f]{20})")) {
            throw new IllegalArgumentException("콘텐츠 ID 형식을 확인해 주세요.");
        }
    }
}
