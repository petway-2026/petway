package com.petway.place.service;

import com.petway.place.dto.PlaceSearchRequest;
import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.dto.PlaceSummaryResponse;
import com.petway.place.dto.NearbyPlaceSearchRequest;
import com.petway.place.model.NearbyPlaceSearchCondition;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.place.model.PlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.port.TourPlaceGateway;
import com.petway.facility.PetFacility;
import com.petway.facility.PetFacilityCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaceQueryService {

    private final TourPlaceGateway tourPlaceGateway;
    private final PetFacilityCatalog facilityCatalog;

    @Autowired
    public PlaceQueryService(TourPlaceGateway tourPlaceGateway, PetFacilityCatalog facilityCatalog) {
        this.tourPlaceGateway = tourPlaceGateway;
        this.facilityCatalog = facilityCatalog;
    }

    public PlaceQueryService(TourPlaceGateway tourPlaceGateway) {
        this(tourPlaceGateway, new PetFacilityCatalog());
    }

    public PlaceSearchResponse searchByArea(PlaceSearchRequest request) {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                request.getAreaCode(),
                request.getSigunguCode(),
                request.getContentTypeId(),
                request.getPage(),
                request.getSize()
        );

        return toResponse(tourPlaceGateway.searchByArea(condition),
                catalog().placesByArea(condition.areaCode(), condition.contentTypeId()), condition.page(), condition.size());
    }

    public PlaceSearchResponse searchNearby(NearbyPlaceSearchRequest request) {
        NearbyPlaceSearchCondition condition = new NearbyPlaceSearchCondition(
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius(),
                request.getPage(),
                request.getSize()
        );

        return toResponse(findNearby(condition),
                catalog().placesNearby(condition.latitude(), condition.longitude(), condition.radius()), condition.page(), condition.size());
    }

    public PlaceSearchResult findNearby(NearbyPlaceSearchCondition condition) {
        return tourPlaceGateway.searchNearby(condition);
    }

    public PlacePetDetailResponse getPetDetail(String contentId) {
        var csv = catalog().byId(contentId);
        if (csv.isPresent()) {
            PetFacility f = csv.get();
            return PlacePetDetailResponse.from(new com.petway.place.model.PlacePetDetail(
                    f.normalizedAccompanimentType(), f.normalizedAllowedCapacity(), null, f.normalizedAdditionalInformation(), null));
        }
        var detail = tourPlaceGateway.getPetDetail(contentId);
        return PlacePetDetailResponse.from(detail);
    }

    private PlaceSearchResponse toResponse(PlaceSearchResult result, java.util.List<PetFacility> localFacilities, int page, int size) {
        var matchedLocalIds = new java.util.HashSet<String>();
        var external = result.items().stream().map(place -> {
            PetFacility metadata = catalog().matching(place.title(), place.address(), place.latitude(), place.longitude()).orElse(null);
            if (metadata != null) matchedLocalIds.add(metadata.id());
            return response(place.contentId(), place.contentTypeId(), place.areaCode(), place.sigunguCode(), place.title(), place.address(),
                    place.latitude(), place.longitude(), place.thumbnailUrl(), metadata, "한국관광공사");
        }).toList();
        int from = Math.min((page - 1) * size, localFacilities.size());
        int to = Math.min(from + size, localFacilities.size());
        var local = localFacilities.subList(from, to).stream()
                .filter(f -> !matchedLocalIds.contains(f.id()))
                .map(f -> response(f.id(), f.contentTypeId(), null, null, f.name(), f.address(), f.latitude(), f.longitude(), null, f, "한국문화정보원"))
                .toList();
        var items = new java.util.ArrayList<PlaceSummaryResponse>(external);
        items.addAll(local);
        return new PlaceSearchResponse(
                result.page(),
                result.size(),
                result.totalCount() + localFacilities.size(),
                items
        );
    }

    private PlaceSummaryResponse response(String id, String type, String area, String district, String title, String address,
                                          java.math.BigDecimal latitude, java.math.BigDecimal longitude, String image,
                                          PetFacility f, String source) {
        return new PlaceSummaryResponse(id, type, area, district, title, address, latitude, longitude, image,
                f == null ? null : f.telephone(), f == null ? null : f.homepage(), f == null ? null : f.operatingHours(),
                f == null ? null : f.closedDays(), f == null ? null : f.parking(), f == null ? null : f.price(),
                f == null ? null : f.indoor(), f == null ? null : f.outdoor(), f == null ? null : f.description(), source);
    }

    private PetFacilityCatalog catalog() { return facilityCatalog == null ? new PetFacilityCatalog() : facilityCatalog; }
}
