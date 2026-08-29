package com.petway.place.controller;

import com.petway.place.dto.PlaceSearchRequest;
import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.dto.NearbyPlaceSearchRequest;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.place.service.PlaceQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceQueryService placeQueryService;

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
        if (!contentId.matches("\\d+")) {
            throw new IllegalArgumentException("콘텐츠 ID 형식을 확인해 주세요.");
        }
        return placeQueryService.getPetDetail(contentId);
    }
}
