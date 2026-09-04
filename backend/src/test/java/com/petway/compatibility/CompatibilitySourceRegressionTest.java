package com.petway.compatibility;

import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.petrule.PetRuleParser;
import com.petway.place.mapper.PlacePetRuleSourceAdapter;
import com.petway.place.model.PlacePetDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilitySourceRegressionTest {

    private final PetRuleParser parser = new PetRuleParser();
    private final PlacePetRuleSourceAdapter adapter = new PlacePetRuleSourceAdapter();
    private final CompatibilityEvaluator evaluator = new CompatibilityEvaluator();

    @Test
    void allBreedsAndIndoorAllowedWithoutRestrictionsIsAvailable() {
        CompatibilityResult result = evaluate(
                pet("8", PetSizeType.MEDIUM),
                detail("모든 견종 동반 가능", null, "실내 동반 가능")
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(result.reasons()).contains(
                "모든 견종의 동반이 허용됩니다.",
                "실내 동반이 허용됩니다."
        );
    }

    @Test
    void allBreedsAndIndoorAllowedWithCarrierIsConditional() {
        CompatibilityResult result = evaluate(
                pet("8", PetSizeType.MEDIUM),
                detail("모든 견종 동반 가능", "이동장 필수", "실내 동반 가능")
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.requirements())
                .extracting(requirement -> requirement.type().name())
                .containsExactly("CARRIER");
    }

    @Test
    void explicitWeightLimitDependsOnPetWeight() {
        PlacePetDetail detail = detail(
                "모든 견종 동반 가능, 반려견 10kg 이하",
                null,
                "실내 동반 가능"
        );

        assertThat(evaluate(pet("8", PetSizeType.MEDIUM), detail).status())
                .isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(evaluate(pet("12", PetSizeType.MEDIUM), detail).status())
                .isEqualTo(CompatibilityStatus.UNAVAILABLE);
    }

    @Test
    void indoorAllowedDoesNotHideUnclearAnimalCondition() {
        CompatibilityResult result = evaluate(
                pet("8", PetSizeType.MEDIUM),
                detail(
                        "동반 가능 동물은 현장 상황에 따라 달라질 수 있음",
                        null,
                        "실내 동반 가능"
                )
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons())
                .contains("동반 가능한 반려동물 조건에 확인되지 않은 내용이 있습니다.");
    }

    @Test
    void conflictingIndoorRulesNeedConfirmation() {
        CompatibilityResult result = evaluate(
                pet("8", PetSizeType.MEDIUM),
                detail(
                        "모든 견종 동반 가능",
                        null,
                        "실내 동반 가능, 실내 동반 불가"
                )
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void explicitSizeProhibitionIsUnavailable() {
        CompatibilityResult result = evaluate(
                pet("8", PetSizeType.SMALL),
                detail("소형견 동반 불가", null, null)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
    }

    private CompatibilityResult evaluate(Pet pet, PlacePetDetail detail) {
        return evaluator.evaluate(pet, parser.parse(adapter.adapt(detail)));
    }

    private PlacePetDetail detail(
            String allowedCapacity,
            String requiredItems,
            String additionalInformation
    ) {
        return new PlacePetDetail(
                null,
                allowedCapacity,
                requiredItems,
                additionalInformation,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Pet pet(String weightKg, PetSizeType sizeType) {
        return Pet.create(
                null,
                "두부",
                null,
                null,
                null,
                null,
                new BigDecimal(weightKg),
                sizeType,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }
}
