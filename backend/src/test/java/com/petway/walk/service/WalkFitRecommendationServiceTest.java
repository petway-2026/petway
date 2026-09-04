package com.petway.walk.service;

import com.petway.compatibility.CompatibilityStatus;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.ActivityLevel;
import com.petway.pet.enums.PlacePreference;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.service.PrimaryPetQueryService;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.place.model.NearbyPlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.PlaceSummary;
import com.petway.place.service.PlaceCompatibilityService;
import com.petway.place.service.PlaceQueryService;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.user.entity.User;
import com.petway.walk.dto.WalkFitRecommendationResponse;
import com.petway.walk.policy.WalkFitPolicy;
import com.petway.walk.policy.WalkPlaceTypePolicy;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.service.WeatherQueryService;
import com.petway.weather.service.WeatherRegionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalkFitRecommendationServiceTest {

    private static final BigDecimal LATITUDE = new BigDecimal("37.5665");
    private static final BigDecimal LONGITUDE = new BigDecimal("126.9780");
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 9, 1);

    @Mock
    private PrimaryPetQueryService primaryPetQueryService;
    @Mock
    private PlaceQueryService placeQueryService;
    @Mock
    private PlaceCompatibilityService placeCompatibilityService;
    @Mock
    private WeatherQueryService weatherQueryService;

    private WalkFitRecommendationService service;
    private Pet primaryPet;

    @BeforeEach
    void setUp() {
        service = new WalkFitRecommendationService(
                primaryPetQueryService,
                placeQueryService,
                placeCompatibilityService,
                weatherQueryService,
                new WeatherRegionResolver(),
                new WalkFitPolicy(new WalkPlaceTypePolicy())
        );
        primaryPet = pet();
        when(weatherQueryService.resolveForecastType(TARGET_DATE))
                .thenReturn(ForecastType.SHORT_TERM);
        when(primaryPetQueryService.findByUserEmail("owner@petway.test"))
                .thenReturn(primaryPet);
    }

    @Test
    void excludesUnavailableCandidate() {
        stubPlaces(List.of(place("100", "28", LATITUDE, LONGITUDE)));
        when(placeCompatibilityService.getCompatibility(primaryPet, "100"))
                .thenReturn(compatibility("100", CompatibilityStatus.UNAVAILABLE, List.of(), false));

        WalkFitRecommendationResponse response = recommend();

        assertThat(response.recommendations()).isEmpty();
        assertThat(response.message()).contains("조건에 맞는 산책 후보");
    }

    @Test
    void retainsConditionalRequirementInRecommendation() {
        var leash = new PlaceCompatibilityResponse.RequirementItem(
                RequirementType.LEASH,
                "목줄",
                SpaceScope.UNSPECIFIED
        );
        stubPlaces(List.of(place("100", "28", LATITUDE, LONGITUDE)));
        when(placeCompatibilityService.getCompatibility(primaryPet, "100"))
                .thenReturn(compatibility(
                        "100",
                        CompatibilityStatus.CONDITIONAL,
                        List.of(leash),
                        false
                ));
        when(weatherQueryService.getForecast(any(Double.class), any(Double.class), eq(TARGET_DATE), any()))
                .thenThrow(new RuntimeException("weather unavailable"));

        WalkFitRecommendationResponse response = recommend();

        var result = response.recommendations().get(0);
        assertThat(result.compatibility().status())
                .isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.compatibility().requirements()).containsExactly(leash);
    }

    @Test
    void keepsCandidateWhenWeatherFailsAndMarksWeatherUnavailable() {
        stubPlaces(List.of(place("100", "28", LATITUDE, LONGITUDE)));
        when(placeCompatibilityService.getCompatibility(primaryPet, "100"))
                .thenReturn(compatibility("100", CompatibilityStatus.AVAILABLE, List.of(), false));
        when(weatherQueryService.getForecast(any(Double.class), any(Double.class), eq(TARGET_DATE), any()))
                .thenThrow(new RuntimeException("weather unavailable"));

        WalkFitRecommendationResponse response = recommend();

        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).weather().available()).isFalse();
        assertThat(response.recommendations().get(0).walkFit().reasons())
                .contains("날씨 정보를 확인하지 못해 날씨는 평가에서 제외했어요.");
    }

    @Test
    void returnsClearMessageWhenAllCandidatesAreUnavailable() {
        stubPlaces(List.of(
                place("100", "28", LATITUDE, LONGITUDE),
                place("200", "12", LATITUDE, LONGITUDE)
        ));
        when(placeCompatibilityService.getCompatibility(eq(primaryPet), anyString()))
                .thenAnswer(invocation -> compatibility(
                        invocation.getArgument(1),
                        CompatibilityStatus.UNAVAILABLE,
                        List.of(),
                        false
                ));

        WalkFitRecommendationResponse response = recommend();

        assertThat(response.recommendations()).isEmpty();
        assertThat(response.message())
                .isEqualTo("현재 반려견 조건에 맞는 산책 후보를 찾지 못했어요.");
    }

    @Test
    void capsCompatibilityFanOutAtEightCandidates() {
        List<PlaceSummary> places = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(index -> place(
                        String.valueOf(100 + index),
                        "28",
                        LATITUDE,
                        LONGITUDE
                ))
                .toList();
        stubPlaces(places);
        when(placeCompatibilityService.getCompatibility(eq(primaryPet), anyString()))
                .thenAnswer(invocation -> compatibility(
                        invocation.getArgument(1),
                        CompatibilityStatus.AVAILABLE,
                        List.of(),
                        false
                ));
        when(weatherQueryService.getForecast(any(Double.class), any(Double.class), eq(TARGET_DATE), any()))
                .thenThrow(new RuntimeException("weather unavailable"));

        WalkFitRecommendationResponse response = recommend();

        ArgumentCaptor<NearbyPlaceSearchCondition> condition =
                ArgumentCaptor.forClass(NearbyPlaceSearchCondition.class);
        verify(placeQueryService).findNearby(condition.capture());
        assertThat(condition.getValue().size())
                .isEqualTo(WalkFitRecommendationService.CANDIDATE_LIMIT);
        verify(placeCompatibilityService, times(8))
                .getCompatibility(eq(primaryPet), anyString());
        assertThat(response.recommendations()).hasSize(8);
        assertThat(response.recommendations())
                .extracting(WalkFitRecommendationResponse.Recommendation::rank)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8);
    }

    @Test
    void returnsAllSixSortedEligibleCandidatesWithoutPadding() {
        List<PlaceSummary> places = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(index -> place(
                        String.valueOf(100 + index),
                        "28",
                        LATITUDE.add(new BigDecimal("0.001").multiply(BigDecimal.valueOf(index))),
                        LONGITUDE
                ))
                .toList();
        stubPlaces(places);
        when(placeCompatibilityService.getCompatibility(eq(primaryPet), anyString()))
                .thenAnswer(invocation -> compatibility(
                        invocation.getArgument(1),
                        CompatibilityStatus.AVAILABLE,
                        List.of(),
                        false
                ));
        when(weatherQueryService.getForecast(any(Double.class), any(Double.class), eq(TARGET_DATE), any()))
                .thenThrow(new RuntimeException("weather unavailable"));

        WalkFitRecommendationResponse response = recommend();

        assertThat(response.recommendations()).hasSize(6);
        assertThat(response.recommendations())
                .extracting(WalkFitRecommendationResponse.Recommendation::rank)
                .containsExactly(1, 2, 3, 4, 5, 6);
        verify(placeCompatibilityService, times(6))
                .getCompatibility(eq(primaryPet), anyString());
    }

    @Test
    void continuesWithSuccessfulCandidatesWhenAnotherCandidateIsRateLimited() {
        stubPlaces(List.of(
                place("100", "28", LATITUDE, LONGITUDE),
                place("200", "28", LATITUDE, LONGITUDE)
        ));
        when(placeCompatibilityService.getCompatibility(primaryPet, "100"))
                .thenReturn(compatibility("100", CompatibilityStatus.AVAILABLE, List.of(), false));
        when(placeCompatibilityService.getCompatibility(primaryPet, "200"))
                .thenThrow(rateLimited());
        when(weatherQueryService.getForecast(any(Double.class), any(Double.class), eq(TARGET_DATE), any()))
                .thenThrow(new RuntimeException("weather unavailable"));

        WalkFitRecommendationResponse response = recommend();

        assertThat(response.recommendations())
                .extracting(WalkFitRecommendationResponse.Recommendation::contentId)
                .containsExactly("100");
    }

    @Test
    void reportsRateLimitWhenEveryCandidateIsRateLimited() {
        stubPlaces(List.of(
                place("100", "28", LATITUDE, LONGITUDE),
                place("200", "28", LATITUDE, LONGITUDE)
        ));
        when(placeCompatibilityService.getCompatibility(eq(primaryPet), anyString()))
                .thenThrow(rateLimited());

        assertThatThrownBy(this::recommend)
                .isInstanceOf(WalkFitRateLimitedException.class)
                .hasMessageContaining("일시적인 요청 한도");
    }

    @Test
    void removesDuplicateContentIdsBeforeCompatibilityLookup() {
        PlaceSummary duplicate = place("100", "28", LATITUDE, LONGITUDE);
        stubPlaces(List.of(duplicate, duplicate));
        when(placeCompatibilityService.getCompatibility(primaryPet, "100"))
                .thenReturn(compatibility("100", CompatibilityStatus.AVAILABLE, List.of(), false));
        when(weatherQueryService.getForecast(any(Double.class), any(Double.class), eq(TARGET_DATE), any()))
                .thenThrow(new RuntimeException("weather unavailable"));

        WalkFitRecommendationResponse response = recommend();

        assertThat(response.recommendations()).hasSize(1);
        verify(placeCompatibilityService, times(1))
                .getCompatibility(primaryPet, "100");
    }

    @Test
    void sortsTiesByDistanceThenContentIdDeterministically() {
        PlaceSummary farther = place(
                "200", "28", new BigDecimal("37.5765"), LONGITUDE
        );
        PlaceSummary nearerWithHigherId = place(
                "300", "28", new BigDecimal("37.5670"), LONGITUDE
        );
        PlaceSummary nearerWithLowerId = place(
                "100", "28", new BigDecimal("37.5670"), LONGITUDE
        );
        stubPlaces(List.of(farther, nearerWithHigherId, nearerWithLowerId));
        when(placeCompatibilityService.getCompatibility(eq(primaryPet), anyString()))
                .thenAnswer(invocation -> compatibility(
                        invocation.getArgument(1),
                        CompatibilityStatus.AVAILABLE,
                        List.of(),
                        false
                ));
        when(weatherQueryService.getForecast(any(Double.class), any(Double.class), eq(TARGET_DATE), any()))
                .thenThrow(new RuntimeException("weather unavailable"));

        WalkFitRecommendationResponse response = recommend();

        assertThat(response.recommendations())
                .extracting(WalkFitRecommendationResponse.Recommendation::contentId)
                .containsExactly("100", "300", "200");
        assertThat(response.recommendations())
                .extracting(WalkFitRecommendationResponse.Recommendation::rank)
                .containsExactly(1, 2, 3);
    }

    private WalkFitRecommendationResponse recommend() {
        return service.recommend(
                "owner@petway.test",
                LATITUDE,
                LONGITUDE,
                TARGET_DATE
        );
    }

    private TourApiException rateLimited() {
        return new TourApiException(
                TourApiErrorType.RATE_LIMITED,
                "provider rate limited"
        );
    }

    private void stubPlaces(List<PlaceSummary> places) {
        when(placeQueryService.findNearby(any()))
                .thenReturn(new PlaceSearchResult(1, places.size(), places.size(), places));
    }

    private PlaceSummary place(
            String contentId,
            String contentTypeId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        return new PlaceSummary(
                contentId,
                contentTypeId,
                "1",
                "1",
                "장소 " + contentId,
                "서울특별시",
                latitude,
                longitude,
                null
        );
    }

    private PlaceCompatibilityResponse compatibility(
            String contentId,
            CompatibilityStatus status,
            List<PlaceCompatibilityResponse.RequirementItem> requirements,
            boolean needsConfirmation
    ) {
        return new PlaceCompatibilityResponse(
                contentId,
                primaryPet.getId(),
                primaryPet.getPetName(),
                new PlacePetDetailResponse(null, null, null, null, null),
                new PlaceCompatibilityResponse.PetwayAssessment(
                        status,
                        List.of(),
                        requirements,
                        needsConfirmation
                )
        );
    }

    private Pet pet() {
        Pet pet = Pet.create(
                User.create("owner@petway.test", "보호자"),
                "앤디",
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("8"),
                PetSizeType.MEDIUM,
                ActivityLevel.HIGH,
                null,
                PlacePreference.OUTDOOR,
                null,
                null,
                null,
                true
        );
        ReflectionTestUtils.setField(pet, "id", 11L);
        return pet;
    }
}
