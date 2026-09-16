/* 관광공사 검색과 문화정보원 보조 목록을 통합하고 중복·정렬·페이지 및 출처별 상세 조회를 처리한다. */
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
import com.petway.place.model.PlaceSummary;
import com.petway.external.tour.exception.TourApiException;
import com.petway.external.tour.exception.TourApiErrorType;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.IntFunction;

@Service
public class PlaceQueryService {

    private final TourPlaceGateway tourPlaceGateway;
    private final PetFacilityCatalog facilityCatalog;
    private final PlaceSearchSnapshotCache snapshotCache;
    private static final int PROVIDER_PAGE_SIZE = 100;
    private static final int MAX_PROVIDER_PAGES = 100;

    @Autowired
    public PlaceQueryService(TourPlaceGateway tourPlaceGateway, PetFacilityCatalog facilityCatalog,
                             PlaceSearchSnapshotCache snapshotCache) {
        this.tourPlaceGateway = tourPlaceGateway;
        this.facilityCatalog = facilityCatalog;
        this.snapshotCache = snapshotCache;
    }

    public PlaceQueryService(TourPlaceGateway tourPlaceGateway, PetFacilityCatalog facilityCatalog) {
        this(tourPlaceGateway, facilityCatalog, new PlaceSearchSnapshotCache(Clock.systemUTC()));
    }

    public PlaceQueryService(TourPlaceGateway tourPlaceGateway) {
        this(tourPlaceGateway, new PetFacilityCatalog());
    }

    // 외부 목록과 보조 목록을 먼저 완성·통합한 뒤 페이지를 자른다. 전체 건수와 페이지 경계를 일치시키기 위한 순서다.
    public PlaceSearchResponse searchByArea(PlaceSearchRequest request) {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                request.getAreaCode(),
                request.getSigunguCode(),
                request.getContentTypeId(),
                1,
                PROVIDER_PAGE_SIZE
        );

        var items = snapshotCache.get(condition, () -> merge(
                loadAll(page -> tourPlaceGateway.searchByArea(new PlaceSearchCondition(
                        condition.areaCode(), condition.sigunguCode(), condition.contentTypeId(), page, PROVIDER_PAGE_SIZE))),
                // The CSV has no KTO district codes; do not add province-wide matches to a district query.
                condition.sigunguCode() == null || condition.sigunguCode().isBlank()
                        ? catalog().placesByArea(condition.areaCode(), condition.contentTypeId()) : List.of()));
        return page(items, request.getPage(), request.getSize());
    }

    // 통합 결과를 반경으로 다시 걸러 거리순으로 정렬한다. 동일 검색 조건의 페이지는 같은 스냅샷을 재사용한다.
    public PlaceSearchResponse searchNearby(NearbyPlaceSearchRequest request) {
        NearbyPlaceSearchCondition condition = new NearbyPlaceSearchCondition(
                request.getLatitude().stripTrailingZeros(),
                request.getLongitude().stripTrailingZeros(),
                request.getRadius(),
                1,
                PROVIDER_PAGE_SIZE
        );

        var items = snapshotCache.get(condition, () -> merge(
                loadAll(page -> findNearby(new NearbyPlaceSearchCondition(condition.latitude(), condition.longitude(),
                        condition.radius(), page, PROVIDER_PAGE_SIZE))),
                catalog().placesNearby(condition.latitude(), condition.longitude(), condition.radius())).stream()
                .filter(place -> distance(condition, place) <= condition.radius())
                .sorted(java.util.Comparator.comparingDouble((PlaceSummaryResponse place) -> distance(condition, place))
                        .thenComparing(PlaceSummaryResponse::contentId))
                .toList());
        return page(items, request.getPage(), request.getSize());
    }

    public PlaceSearchResult findNearby(NearbyPlaceSearchCondition condition) {
        return tourPlaceGateway.searchNearby(condition);
    }

    /**
     * Nearby candidates for recommendation flows.  Unlike the paginated
     * provider-only lookup above, this merges the local CSV catalog so a
     * temporary tourism API outage does not remove all local recommendations.
     */
    public PlaceSearchResult findNearbyIncludingCatalog(NearbyPlaceSearchCondition condition) {
        var merged = new LinkedHashMap<String, PlaceSummary>();
        // 추천용 조회에서는 관광공사 실패를 잠시 보존하고 독립된 CSV 후보를 확인한다. 양쪽 모두 비면 원래 오류를 전달한다.
        RuntimeException providerFailure = null;
        try {
            PlaceSearchResult external = findNearby(condition);
            if (external != null) {
                external.items().forEach(place -> merged.putIfAbsent(place.contentId(), place));
            }
        } catch (RuntimeException exception) {
            // The local catalog is an independent source. Keep using it when
            // the tourism provider is rate limited or temporarily unavailable.
            providerFailure = exception;
        }
        catalog().placeSummariesNearby(condition.latitude(), condition.longitude(), condition.radius())
                .forEach(place -> merged.putIfAbsent(place.contentId(), place));

        if (merged.isEmpty() && providerFailure != null) {
            throw providerFailure;
        }

        var sorted = merged.values().stream()
                .filter(place -> place.latitude() != null && place.longitude() != null)
                .sorted(java.util.Comparator
                        .comparingDouble((PlaceSummary place) -> distance(condition, place))
                        .thenComparing(PlaceSummary::contentId))
                .toList();
        return new PlaceSearchResult(1, sorted.size(), sorted.size(), sorted);
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

    // 페이지 중간에 전체 건수가 달라지거나 예상 개수가 누락되면 불완전한 목록을 정상 결과로 캐시하지 않는다.
    private List<PlaceSummary> loadAll(IntFunction<PlaceSearchResult> fetchPage) {
        var items = new ArrayList<PlaceSummary>();
        int expectedTotal = -1;
        for (int page = 1; page <= MAX_PROVIDER_PAGES; page++) {
            PlaceSearchResult result = fetchPage.apply(page);
            if (expectedTotal < 0) expectedTotal = result.totalCount();
            if (expectedTotal != result.totalCount() || expectedTotal < 0
                    || expectedTotal > MAX_PROVIDER_PAGES * PROVIDER_PAGE_SIZE) {
                throw incompleteSearch();
            }
            int expectedSize = Math.min(PROVIDER_PAGE_SIZE, expectedTotal - items.size());
            if (result.items().size() != expectedSize) throw incompleteSearch();
            items.addAll(result.items());
            if (items.size() == expectedTotal) return items;
        }
        throw incompleteSearch();
    }

    private TourApiException incompleteSearch() {
        return new TourApiException(TourApiErrorType.INVALID_RESPONSE,
                "장소 검색 결과를 완전히 가져오지 못했습니다. 검색 범위를 줄이거나 잠시 후 다시 시도해 주세요.");
    }

    private PlaceSearchResponse page(List<PlaceSummaryResponse> items, int page, int size) {
        long offset = (long) (page - 1) * size;
        int start = (int) Math.min(offset, items.size());
        int end = Math.min(start + size, items.size());
        return new PlaceSearchResponse(page, size, items.size(), List.copyOf(items.subList(start, end)));
    }

    private double distance(NearbyPlaceSearchCondition condition, PlaceSummaryResponse place) {
        if (place.latitude() == null || place.longitude() == null) return Double.POSITIVE_INFINITY;
        double latitude = Math.toRadians(condition.latitude().doubleValue());
        double otherLatitude = Math.toRadians(place.latitude().doubleValue());
        double deltaLatitude = otherLatitude - latitude;
        double deltaLongitude = Math.toRadians(place.longitude().subtract(condition.longitude()).doubleValue());
        double a = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(latitude) * Math.cos(otherLatitude) * Math.pow(Math.sin(deltaLongitude / 2), 2);
        return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    private double distance(NearbyPlaceSearchCondition condition, PlaceSummary place) {
        if (place.latitude() == null || place.longitude() == null) return Double.POSITIVE_INFINITY;
        double latitude = Math.toRadians(condition.latitude().doubleValue());
        double otherLatitude = Math.toRadians(place.latitude().doubleValue());
        double deltaLatitude = otherLatitude - latitude;
        double deltaLongitude = Math.toRadians(place.longitude().subtract(condition.longitude()).doubleValue());
        double a = Math.pow(Math.sin(deltaLatitude / 2), 2)
                + Math.cos(latitude) * Math.cos(otherLatitude) * Math.pow(Math.sin(deltaLongitude / 2), 2);
        return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }

    // 관광공사 항목을 기준으로 같은 시설의 CSV 메타데이터를 보강하고, 연결되지 않은 CSV 시설만 추가한다.
    private List<PlaceSummaryResponse> merge(List<PlaceSummary> externalPlaces, List<PetFacility> localFacilities) {
        var matchedLocalIds = new java.util.HashSet<String>();
        var items = new LinkedHashMap<String, PlaceSummaryResponse>();
        for (PlaceSummary place : externalPlaces) {
            if (items.containsKey(place.contentId())) continue;
            PetFacility metadata = catalog().matching(place.title(), place.address(), place.latitude(), place.longitude()).orElse(null);
            if (metadata != null && !matchedLocalIds.add(metadata.id())) continue;
            items.put(place.contentId(), response(place.contentId(), place.contentTypeId(), place.areaCode(), place.sigunguCode(), place.title(), place.address(),
                    place.latitude(), place.longitude(), place.thumbnailUrl(), metadata, "한국관광공사"));
        }
        for (PetFacility f : localFacilities) {
            if (!matchedLocalIds.contains(f.id())) {
                items.putIfAbsent(f.id(), response(f.id(), f.contentTypeId(), null, null, f.name(), f.address(), f.latitude(), f.longitude(), null, f, "한국문화정보원"));
            }
        }
        return List.copyOf(items.values());
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
