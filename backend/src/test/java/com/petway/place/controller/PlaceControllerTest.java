package com.petway.place.controller;

import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.dto.PlaceSummaryResponse;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.compatibility.CompatibilityStatus;
import com.petway.place.service.PlaceCompatibilityService;
import com.petway.place.service.PlaceQueryService;
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
}
