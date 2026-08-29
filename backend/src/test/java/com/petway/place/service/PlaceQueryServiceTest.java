package com.petway.place.service;

import com.petway.place.dto.PlaceSearchRequest;
import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.PlaceSummary;
import com.petway.place.port.TourPlaceGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private PlaceQueryService placeQueryService;

    @Test
    void mapsRequestAndGatewayResultToApiResponse() {
        PlaceSearchRequest request = new PlaceSearchRequest();
        request.setAreaCode("1");
        request.setSigunguCode("1");
        request.setContentTypeId("12");
        request.setPage(2);
        request.setSize(10);

        when(tourPlaceGateway.searchByArea(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PlaceSearchResult(
                        2,
                        10,
                        1,
                        List.of(new PlaceSummary(
                                "125266",
                                "12",
                                "경복궁",
                                "서울특별시 종로구 사직로 161",
                                new BigDecimal("37.5788222356"),
                                new BigDecimal("126.9769930325"),
                                null
                        ))
                ));

        PlaceSearchResponse response = placeQueryService.searchByArea(request);

        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.items()).extracting("contentId")
                .containsExactly("125266");

        ArgumentCaptor<com.petway.place.model.PlaceSearchCondition> captor =
                ArgumentCaptor.forClass(
                        com.petway.place.model.PlaceSearchCondition.class
                );
        verify(tourPlaceGateway).searchByArea(captor.capture());
        assertThat(captor.getValue().areaCode()).isEqualTo("1");
        assertThat(captor.getValue().page()).isEqualTo(2);
        assertThat(captor.getValue().size()).isEqualTo(10);
    }
}
