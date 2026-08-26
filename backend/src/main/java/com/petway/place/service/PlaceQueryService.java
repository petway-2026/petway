package com.petway.place.service;

import com.petway.place.dto.PlaceSearchRequest;
import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.dto.PlaceSummaryResponse;
import com.petway.place.model.PlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.port.TourPlaceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceQueryService {

    private final TourPlaceGateway tourPlaceGateway;

    public PlaceSearchResponse searchByArea(PlaceSearchRequest request) {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                request.getAreaCode(),
                request.getSigunguCode(),
                request.getContentTypeId(),
                request.getPage(),
                request.getSize()
        );

        PlaceSearchResult result = tourPlaceGateway.searchByArea(condition);

        return new PlaceSearchResponse(
                result.page(),
                result.size(),
                result.totalCount(),
                result.items().stream()
                        .map(place -> new PlaceSummaryResponse(
                                place.contentId(),
                                place.contentTypeId(),
                                place.title(),
                                place.address(),
                                place.latitude(),
                                place.longitude(),
                                place.thumbnailUrl()
                        ))
                        .toList()
        );
    }
}
