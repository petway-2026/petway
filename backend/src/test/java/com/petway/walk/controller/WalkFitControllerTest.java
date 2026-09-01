package com.petway.walk.controller;

import com.petway.global.exception.GlobalExceptionHandler;
import com.petway.walk.dto.WalkFitRecommendationResponse;
import com.petway.walk.service.WalkFitRecommendationService;
import com.petway.walk.service.WalkFitRateLimitedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalkFitControllerTest {

    @Mock
    private WalkFitRecommendationService recommendationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalkFitController(recommendationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void usesAuthenticatedUserWithoutAcceptingPetIdFromFrontend() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 9, 1);
        when(recommendationService.recommend(
                "owner@petway.test",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                targetDate
        )).thenReturn(new WalkFitRecommendationResponse(
                targetDate,
                new WalkFitRecommendationResponse.PetSummary(11L, "앤디"),
                List.of(),
                "주변에서 산책 후보 장소를 찾지 못했어요."
        ));

        mockMvc.perform(get("/api/walk/recommendations")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-09-01")
                        .principal(new TestingAuthenticationToken(
                                "owner@petway.test",
                                null
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pet.petId").value(11))
                .andExpect(jsonPath("$.pet.petName").value("앤디"));

        verify(recommendationService).recommend(
                "owner@petway.test",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                targetDate
        );
    }

    @Test
    void rejectsCoordinatesOutsideSupportedTourArea() throws Exception {
        mockMvc.perform(get("/api/walk/recommendations")
                        .param("latitude", "40")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-09-01")
                        .principal(new TestingAuthenticationToken(
                                "owner@petway.test",
                                null
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedDate() throws Exception {
        mockMvc.perform(get("/api/walk/recommendations")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "not-a-date")
                        .principal(new TestingAuthenticationToken(
                                "owner@petway.test",
                                null
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsDistinctApplicationErrorWhenProviderIsRateLimited() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 9, 1);
        when(recommendationService.recommend(
                "owner@petway.test",
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                targetDate
        )).thenThrow(new WalkFitRateLimitedException(
                "관광정보 제공기관의 일시적인 요청 한도로 추천을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
        ));

        mockMvc.perform(get("/api/walk/recommendations")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780")
                        .param("targetDate", "2026-09-01")
                        .principal(new TestingAuthenticationToken(
                                "owner@petway.test",
                                null
                        )))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code")
                        .value("WALK_FIT_PROVIDER_RATE_LIMITED"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("일시적인 요청 한도")));
    }
}
