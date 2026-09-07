package com.petway.compatibility;

import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.enums.DogBreed;
import com.petway.petrule.PetRule;
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"2586384", "264311", "264353"})
    void verifiesActualKtoSnapshotForOrdinaryDangerousAndUnknownBreed(String contentId) throws Exception {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode snapshot = null;
        try (var input = getClass().getResourceAsStream("/compatibility/kto-2026-09-08.json")) {
            assertThat(input).isNotNull();
            for (var entry : json.readTree(input)) {
                if (contentId.equals(entry.path("contentId").asText())) snapshot = entry;
            }
        }
        assertThat(snapshot).isNotNull();
        var apiResponse = json.treeToValue(snapshot.path("response"),
                com.petway.external.tour.dto.TourApiResponse.class);
        PlacePetDetail detail = new com.petway.external.tour.mapper.TourPlaceMapper(json)
                .toPlacePetDetail(apiResponse);
        assertThat(detail.additionalInformation()).contains("맹견의 경우, 입마개 착용 필수");
        var rule = parser.parse(adapter.adapt(detail));
        assertThat(rule.sources()).extracting(PetRule.PetRuleSource::rawText)
                .contains(detail.additionalInformation());
        Pet pet = pet("9", PetSizeType.SMALL);
        org.springframework.test.util.ReflectionTestUtils.setField(pet, "breed", DogBreed.MALTESE);
        var ordinary = evaluator.evaluate(pet, rule);
        assertThat(ordinary.status()).isEqualTo("264311".equals(contentId)
                ? CompatibilityStatus.NEED_CONFIRMATION : CompatibilityStatus.CONDITIONAL);
        assertThat(ordinary.requirements()).extracting(r -> r.type().name())
                .contains("LEASH", "WASTE_BAG", "WASTE_DISPOSAL").doesNotContain("MUZZLE");
        org.springframework.test.util.ReflectionTestUtils.setField(pet, "breed", DogBreed.ROTTWEILER);
        assertThat(evaluator.evaluate(pet, rule).requirements()).extracting(r -> r.type().name())
                .contains("MUZZLE");
        org.springframework.test.util.ReflectionTestUtils.setField(pet, "breed", null);
        var unknown = evaluator.evaluate(pet, rule);
        assertThat(unknown.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(unknown.requirements()).extracting(r -> r.type().name()).doesNotContain("MUZZLE");
        if ("2586384".equals(contentId)) {
            org.springframework.test.util.ReflectionTestUtils.setField(pet, "weightKg", new BigDecimal("10"));
            assertThat(evaluator.evaluate(pet, rule).status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(DogBreed.class)
    void ktoDangerousBreedClauseOnlyRequiresMuzzleForMatchingBreed(DogBreed breed) {
        Pet pet = pet("8", PetSizeType.MEDIUM);
        org.springframework.test.util.ReflectionTestUtils.setField(pet, "breed", breed);
        PlacePetDetail detail = detail("모든 견종 동반 가능",
                "목줄 착용, 맹견의 경우, 입마개 착용 필수, 배변봉투 지참",
                "실내 동반 가능");
        PetRule rule = parser.parse(adapter.adapt(detail));
        CompatibilityResult result = evaluator.evaluate(pet, rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.requirements()).extracting(r -> r.type().name())
                .contains("LEASH", "WASTE_BAG");
        assertThat(result.requirements().stream()
                .anyMatch(r -> r.type() == PetRule.RequirementType.MUZZLE))
                .isEqualTo(breed.isDangerous());
        assertThat(rule.sources()).extracting(PetRule.PetRuleSource::rawText)
                .contains("목줄 착용, 맹견의 경우, 입마개 착용 필수, 배변봉투 지참");
        assertThat(rule.requirements().stream()
                .filter(r -> r.type() == PetRule.RequirementType.WASTE_BAG)
                .findFirst().orElseThrow().target().breedGroup())
                .isEqualTo(PetRule.BreedGroup.UNSPECIFIED);
    }

    @Test
    void unknownBreedRequiresConfirmationWithoutUniversalMuzzle() {
        CompatibilityResult result = evaluate(pet("8", PetSizeType.MEDIUM),
                detail("모든 견종 동반 가능", "맹견의 경우, 입마개 착용 필수", "실내 동반 가능"));
        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.requirements()).isEmpty();
    }

    @Test
    void generalMuzzleRequirementStillAppliesToOrdinaryDog() {
        Pet pet = pet("8", PetSizeType.MEDIUM);
        org.springframework.test.util.ReflectionTestUtils.setField(pet, "breed", DogBreed.MALTESE);
        CompatibilityResult result = evaluate(pet,
                detail("모든 견종 동반 가능", "입마개 착용 필수", "실내 동반 가능"));
        assertThat(result.requirements()).extracting(r -> r.type().name()).containsExactly("MUZZLE");
    }

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
