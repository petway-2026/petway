package com.petway.place.controller;

import com.petway.place.dto.PlaceSearchRequest;
import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.service.PlaceQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
