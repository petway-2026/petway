package com.petway.place.controller;

import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.dto.PlaceSummaryResponse;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.compatibility.CompatibilityStatus;
import com.petway.place.service.PlaceCompatibilityService;
import com.petway.place.service.PlaceQueryService;
import com.petway.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlaceControllerTest {

    @Mock
    private PlaceQueryService placeQueryService;

    @Mock
    private PlaceCompatibilityService placeCompatibilityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaceController(
                        placeQueryService,
                        placeCompatibilityService
                ))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsPlaceSearchResponse() throws Exception {
        when(placeQueryService.searchByArea(any()))
                .thenReturn(new PlaceSearchResponse(
                        1,
                        20,
                        1,
                        List.of(new PlaceSummaryResponse(
                                "125266",
                                "12",
                                "1",
                                "1",
                                "경복궁",
                                "서울특별시 종로구 사직로 161",
                                null,
                                null,
                                null
                        ))
                ));

        mockMvc.perform(get("/api/places")
                        .param("areaCode", "1")
                        .param("contentTypeId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].contentId").value("125266"))
                .andExpect(jsonPath("$.items[0].areaCode").value("1"))
                .andExpect(jsonPath("$.items[0].sigunguCode").value("1"));
    }

    @Test
    void rejectsRequestWithoutAreaCode() throws Exception {
        mockMvc.perform(get("/api/places"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAreaCodeThatExceedsTwoDigits() throws Exception {
        mockMvc.perform(get("/api/places")
                        .param("areaCode", "92034254717780654844"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsCompatibilityForAuthenticatedPrincipal() throws Exception {
        when(placeCompatibilityService.getCompatibility(
                "owner@petway.test",
                "125266"
        )).thenReturn(new PlaceCompatibilityResponse(
                "125266",
                11L,
                "보리",
                new PlacePetDetailResponse(
                        "전구역 동반가능",
                        "10kg 이하 동반 가능",
                        "목줄 필수",
                        null,
                        null
                ),
                new PlaceCompatibilityResponse.PetwayAssessment(
                        CompatibilityStatus.CONDITIONAL,
                        List.of("체중 조건을 충족합니다."),
                        List.of(new PlaceCompatibilityResponse.RequirementItem(
                                com.petway.petrule.PetRule.RequirementType.LEASH,
                                "목줄",
                                com.petway.petrule.PetRule.SpaceScope.UNSPECIFIED
                        )),
                        false
                )
        ));

        mockMvc.perform(get("/api/places/125266/compatibility")
                        .principal(new TestingAuthenticationToken(
                                "owner@petway.test",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value("125266"))
                .andExpect(jsonPath("$.petId").value(11))
                .andExpect(jsonPath("$.petName").value("보리"))
                .andExpect(jsonPath("$.petwayAssessment.status")
                        .value("CONDITIONAL"))
                .andExpect(jsonPath("$.petwayAssessment.requirements[0].label")
                        .value("목줄"))
                .andExpect(jsonPath("$.officialInformation.accompanimentType")
                        .value("전구역 동반가능"));
    }

    @Test
    void acceptsCsvFacilityIdForDetailLookup() throws Exception {
        String contentId = "csv-0123456789abcdefabcd";
        when(placeQueryService.getPetDetail(contentId)).thenReturn(new PlacePetDetailResponse(
                "모든 견종 동반 가능", "모든 견종 동반 가능", null, null, null
        ));

        mockMvc.perform(get("/api/places/{contentId}/pet-detail", contentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accompanimentType").value("모든 견종 동반 가능"));

        verify(placeQueryService).getPetDetail(contentId);
    }

    @Test
    void acceptsCsvFacilityIdForCompatibilityLookup() throws Exception {
        String contentId = "csv-0123456789abcdefabcd";

        mockMvc.perform(get("/api/places/{contentId}/compatibility", contentId)
                        .principal(new TestingAuthenticationToken(
                                "owner@petway.test",
                                null
                        )))
                .andExpect(status().isOk());

        verify(placeCompatibilityService).getCompatibility(
                "owner@petway.test",
                contentId
        );
    }

    @Test
    void returnsPreservedPetDetailFields() throws Exception {
        when(placeQueryService.getPetDetail("125266")).thenReturn(new PlacePetDetailResponse(
                "전구역 동반가능",
                null,
                null,
                null,
                null,
                "반려견 놀이터",
                "배변봉투",
                "반려견 간식",
                "반려견 유모차"
        ));

        mockMvc.perform(get("/api/places/{contentId}/pet-detail", "125266"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableFacilities").value("반려견 놀이터"))
                .andExpect(jsonPath("$.providedItems").value("배변봉투"))
                .andExpect(jsonPath("$.purchasableItems").value("반려견 간식"))
                .andExpect(jsonPath("$.rentableItems").value("반려견 유모차"));
    }

    @Test
    void rejectsUnknownContentIdFormat() throws Exception {
        mockMvc.perform(get("/api/places/{contentId}/pet-detail", "csv-invalid"))
                .andExpect(status().isBadRequest());
    }
}
