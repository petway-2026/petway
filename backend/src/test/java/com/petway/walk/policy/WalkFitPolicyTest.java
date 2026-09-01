package com.petway.walk.policy;

import com.petway.compatibility.CompatibilityStatus;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.ActivityLevel;
import com.petway.pet.enums.PlacePreference;
import com.petway.pet.enums.PetSizeType;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.user.entity.User;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.dto.WeatherDisplay;
import com.petway.weather.dto.WeatherForecastResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WalkFitPolicyTest {

    private final WalkFitPolicy policy = new WalkFitPolicy(new WalkPlaceTypePolicy());
    private final Pet outdoorHighPet = pet(PlacePreference.OUTDOOR, ActivityLevel.HIGH);

    @Test
    void givesHighScoreToOutdoorHighActivityPetWithAvailableActivePlaceAndClearWeather() {
        WalkFitPolicy.Result result = policy.evaluate(
                outdoorHighPet,
                "28",
                assessment(CompatibilityStatus.AVAILABLE, List.of(), false),
                weather(WeatherCondition.CLEAR, 10)
        );

        assertThat(result.score()).isGreaterThanOrEqualTo(85);
        assertThat(result.grade()).isEqualTo(WalkFitGrade.VERY_GOOD);
        assertThat(result.reasons()).anyMatch(reason -> reason.contains("높은 활동량"));
    }

    @Test
    void keepsConditionalPlaceAndItsRequirementOutsideTheScoreCalculation() {
        var requirement = new PlaceCompatibilityResponse.RequirementItem(
                RequirementType.LEASH,
                "목줄",
                SpaceScope.UNSPECIFIED
        );
        var compatibility = assessment(
                CompatibilityStatus.CONDITIONAL,
                List.of(requirement),
                false
        );

        WalkFitPolicy.Result result = policy.evaluate(
                outdoorHighPet,
                "28",
                compatibility,
                weather(WeatherCondition.CLEAR, 10)
        );

        assertThat(result.score()).isPositive();
        assertThat(compatibility.requirements()).containsExactly(requirement);
    }

    @Test
    void penalizesNeedConfirmationWithoutChangingTheStatus() {
        var available = assessment(CompatibilityStatus.AVAILABLE, List.of(), false);
        var confirmation = assessment(CompatibilityStatus.NEED_CONFIRMATION, List.of(), true);

        int availableScore = policy.evaluate(
                outdoorHighPet, "28", available, weather(WeatherCondition.CLEAR, 10)
        ).score();
        int confirmationScore = policy.evaluate(
                outdoorHighPet, "28", confirmation, weather(WeatherCondition.CLEAR, 10)
        ).score();

        assertThat(confirmationScore).isLessThan(availableScore);
        assertThat(confirmation.needsConfirmation()).isTrue();
        assertThat(confirmation.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void penalizesRainAndAddsCautionReason() {
        int clearScore = policy.evaluate(
                outdoorHighPet,
                "28",
                assessment(CompatibilityStatus.AVAILABLE, List.of(), false),
                weather(WeatherCondition.CLEAR, 10)
        ).score();
        WalkFitPolicy.Result rainy = policy.evaluate(
                outdoorHighPet,
                "28",
                assessment(CompatibilityStatus.AVAILABLE, List.of(), false),
                weather(WeatherCondition.RAIN, 80)
        );

        assertThat(rainy.score()).isLessThan(clearScore);
        assertThat(rainy.reasons()).anyMatch(reason -> reason.contains("비나 눈 예보"));
    }

    @Test
    void excludesUnavailableWeatherFromTheDenominator() {
        WalkFitPolicy.Result result = policy.evaluate(
                outdoorHighPet,
                "28",
                assessment(CompatibilityStatus.AVAILABLE, List.of(), false),
                null
        );

        assertThat(result.score()).isPositive();
        assertThat(result.reasons())
                .contains("날씨 정보를 확인하지 못해 날씨는 평가에서 제외했어요.");
    }

    private PlaceCompatibilityResponse.PetwayAssessment assessment(
            CompatibilityStatus status,
            List<PlaceCompatibilityResponse.RequirementItem> requirements,
            boolean needsConfirmation
    ) {
        return new PlaceCompatibilityResponse.PetwayAssessment(
                status,
                List.of(),
                requirements,
                needsConfirmation
        );
    }

    private WeatherForecastResponse weather(WeatherCondition condition, int rainProbability) {
        return new WeatherForecastResponse(
                LocalDate.of(2026, 9, 1),
                ForecastType.SHORT_TERM,
                "서울ㆍ인천ㆍ경기도",
                15.0,
                23.0,
                rainProbability,
                condition,
                new WeatherDisplay("15~23℃", "강수확률 " + rainProbability + "%", "맑음", "날씨 요약"),
                null,
                null,
                null,
                null,
                null,
                null,
                "기상청"
        );
    }

    private Pet pet(PlacePreference preference, ActivityLevel activityLevel) {
        return Pet.create(
                User.create("owner@petway.test", "보호자"),
                "앤디",
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("8"),
                PetSizeType.MEDIUM,
                activityLevel,
                null,
                preference,
                null,
                null,
                null,
                true
        );
    }
}
