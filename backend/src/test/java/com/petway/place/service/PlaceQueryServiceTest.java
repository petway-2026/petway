package com.petway.place.service;

import com.petway.place.dto.PlaceSearchRequest;
import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.PlaceSummary;
import com.petway.place.port.TourPlaceGateway;
import com.petway.facility.PetFacility;
import com.petway.facility.PetFacilityCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceQueryServiceTest {

    @Mock
    private TourPlaceGateway tourPlaceGateway;

    @Mock
    private PetFacilityCatalog facilityCatalog;

    private PlaceQueryService placeQueryService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        placeQueryService = new PlaceQueryService(tourPlaceGateway, facilityCatalog);
    }

    @Test
    void mapsRequestAndGatewayResultToApiResponse() {
        PlaceSearchRequest request = new PlaceSearchRequest();
        request.setAreaCode("1");
        request.setSigunguCode("1");
        request.setContentTypeId("12");
        request.setPage(1);
        request.setSize(10);

        when(tourPlaceGateway.searchByArea(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PlaceSearchResult(
                        1,
                        100,
                        1,
                        List.of(new PlaceSummary(
                                "125266",
                                "12",
                                "1",
                                "1",
                                "경복궁",
                                "서울특별시 종로구 사직로 161",
                                new BigDecimal("37.5788222356"),
                                new BigDecimal("126.9769930325"),
                                null
                        ))
                ));

        PlaceSearchResponse response = placeQueryService.searchByArea(request);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.items()).extracting("contentId")
                .containsExactly("125266");
        assertThat(response.items().get(0).areaCode()).isEqualTo("1");
        assertThat(response.items().get(0).sigunguCode()).isEqualTo("1");

        ArgumentCaptor<com.petway.place.model.PlaceSearchCondition> captor =
                ArgumentCaptor.forClass(
                        com.petway.place.model.PlaceSearchCondition.class
                );
        verify(tourPlaceGateway).searchByArea(captor.capture());
        assertThat(captor.getValue().areaCode()).isEqualTo("1");
        assertThat(captor.getValue().page()).isEqualTo(1);
        assertThat(captor.getValue().size()).isEqualTo(100);
        assertThat(captor.getValue().sigunguCode()).isEqualTo("1");
        org.mockito.Mockito.verify(facilityCatalog, org.mockito.Mockito.never()).placesByArea(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mixedSourcesNeverExceedRequestedPageSize() {
        placeQueryService = new PlaceQueryService(tourPlaceGateway, facilityCatalog);
        PlaceSearchRequest request = new PlaceSearchRequest();
        request.setAreaCode("1");
        request.setPage(1);
        request.setSize(2);
        when(tourPlaceGateway.searchByArea(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PlaceSearchResult(1, 2, 2, List.of(
                        place("1", "첫 번째"), place("2", "두 번째")
                )));
        when(facilityCatalog.placesByArea("1", null)).thenReturn(List.of(
                local("csv-1", "로컬 첫 번째"), local("csv-2", "로컬 두 번째")
        ));
        when(facilityCatalog.matching(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());

        PlaceSearchResponse response = placeQueryService.searchByArea(request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items()).extracting("contentId").containsExactly("1", "2");
        assertThat(response.totalCount()).isEqualTo(4);
    }

    @Test
    void deduplicatesBeforePaginationAndUsesSameSnapshotForOutOfOrderPages() {
        var matched = local("csv-1", "첫 번째");
        when(tourPlaceGateway.searchByArea(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PlaceSearchResult(1, 100, 4, List.of(
                        place("1", "첫 번째"), place("2", "두 번째"), place("1", "첫 번째"), place("3", "세 번째"))));
        when(facilityCatalog.placesByArea("1", null)).thenReturn(List.of(
                matched, local("csv-2", "로컬 두 번째"), local("csv-3", "로컬 세 번째")));
        when(facilityCatalog.matching("첫 번째", "서울", BigDecimal.ONE, BigDecimal.ONE))
                .thenReturn(java.util.Optional.of(matched));
        PlaceSearchRequest request = new PlaceSearchRequest();
        request.setAreaCode("1");
        request.setSize(2);
        request.setPage(2);
        var second = placeQueryService.searchByArea(request);
        request.setPage(1);
        var first = placeQueryService.searchByArea(request);
        request.setPage(3);
        var third = placeQueryService.searchByArea(request);
        assertThat(first.totalCount()).isEqualTo(5);
        assertThat(second.totalCount()).isEqualTo(5);
        assertThat(third.totalCount()).isEqualTo(5);
        assertThat(first.items()).extracting("contentId").containsExactly("1", "2");
        assertThat(second.items()).extracting("contentId").containsExactly("3", "csv-2");
        assertThat(third.items()).extracting("contentId").containsExactly("csv-3");
        request.setPage(Integer.MAX_VALUE);
        assertThat(placeQueryService.searchByArea(request).items()).isEmpty();
        verify(tourPlaceGateway).searchByArea(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loadsAllProviderPagesAndDeduplicatesTheirBoundary() {
        var first = java.util.stream.IntStream.rangeClosed(1, 100)
                .mapToObj(id -> place(String.valueOf(id), "장소 " + id)).toList();
        when(tourPlaceGateway.searchByArea(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            var condition = (com.petway.place.model.PlaceSearchCondition) invocation.getArgument(0);
            return new PlaceSearchResult(condition.page(), 100, 101,
                    condition.page() == 1 ? first : List.of(first.get(99)));
        });
        PlaceSearchRequest request = new PlaceSearchRequest();
        request.setAreaCode("1");
        request.setPage(5);
        request.setSize(20);
        var response = placeQueryService.searchByArea(request);
        assertThat(response.totalCount()).isEqualTo(100);
        assertThat(response.items()).hasSize(20);
        assertThat(response.items().get(0).contentId()).isEqualTo("81");
        org.mockito.Mockito.verify(tourPlaceGateway, org.mockito.Mockito.times(2))
                .searchByArea(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void incompleteProviderResponseIsNotCachedAsCompleteResult() {
        when(tourPlaceGateway.searchByArea(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PlaceSearchResult(1, 100, 2, List.of(place("1", "첫 번째"))))
                .thenReturn(new PlaceSearchResult(1, 100, 1, List.of(place("1", "첫 번째"))));
        PlaceSearchRequest request = new PlaceSearchRequest();
        request.setAreaCode("1");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> placeQueryService.searchByArea(request))
                .isInstanceOf(com.petway.external.tour.exception.TourApiException.class);
        assertThat(placeQueryService.searchByArea(request).totalCount()).isEqualTo(1);
    }

    @Test
    void nearbyMergesBothSourcesByDistanceAndRemovesOutOfRadiusResults() {
        var request = new com.petway.place.dto.NearbyPlaceSearchRequest();
        request.setLatitude(BigDecimal.ONE);
        request.setLongitude(BigDecimal.ONE);
        request.setRadius(5000);
        request.setSize(2);
        when(tourPlaceGateway.searchNearby(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PlaceSearchResult(1, 100, 2, List.of(
                        new PlaceSummary("far", "12", "1", null, "멀리", "서울", new BigDecimal("1.01"), BigDecimal.ONE, null),
                        new PlaceSummary("outside", "12", "1", null, "반경 밖", "서울", new BigDecimal("2"), BigDecimal.ONE, null))));
        when(facilityCatalog.placesNearby(BigDecimal.ONE, BigDecimal.ONE, 5000))
                .thenReturn(List.of(local("csv-near", "가까이")));
        var response = placeQueryService.searchNearby(request);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.items()).extracting("contentId").containsExactly("csv-near", "far");
    }

    private PlaceSummary place(String id, String title) {
        return new PlaceSummary(id, "12", "1", null, title, "서울", BigDecimal.ONE, BigDecimal.ONE, null);
    }

    private PetFacility local(String id, String title) {
        return new PetFacility(id, title, "문화시설", "박물관", null, null, "서울", null,
                BigDecimal.ONE, BigDecimal.ONE, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
    }
}
