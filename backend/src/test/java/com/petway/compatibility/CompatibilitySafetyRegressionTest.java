package com.petway.compatibility;

import com.petway.facility.PetFacility;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.DogBreed;
import com.petway.pet.enums.PetSizeType;
import com.petway.petrule.PetRule;
import com.petway.petrule.PetRuleParser;
import com.petway.place.mapper.PlacePetRuleSourceAdapter;
import com.petway.place.model.PlacePetDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** CSV에서 발견한 표현과 규정 충돌·필드 이동 사례를 원문부터 최종 판정까지 검증한다. */
class CompatibilitySafetyRegressionTest {
    private final PetRuleParser parser = new PetRuleParser();
    private final CompatibilityEvaluator evaluator = new CompatibilityEvaluator();

    @Test
    void allBreedsAloneIsAvailableWithoutBlanketDisplayReason() {
        CompatibilityResult result = evaluator.evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                parser.parse(PetRule.PetRuleSourceField.ACMPY_PSBL_CPAM, "모든 견종 동반 가능"));
        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(result.reasons()).isEmpty();
        assertThat(result.reasonGroups().confirmation()).isEmpty();
    }

    @Test
    void breedAllowanceStillCountsWhenOnlyGeneralPolicyNoticeRemains() {
        CompatibilityResult result = evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                "반려견 동반 운영 정책은 사정에 따라 변동 될 수 있음");
        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    void unclearRestrictionExplainsExactlyWhyBreedAllowanceIsInsufficient() {
        CompatibilityResult result = evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), "중성화 필수");
        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasonGroups().confirmation()).hasSize(1)
                .allMatch(reason -> reason.contains("원문: “중성화 필수”"));
        assertThat(result.reasons()).doesNotContain("모든 견종의 동반이 허용됩니다.");
    }

    @Test
    void confirmedAllowanceIsSeparateFromActualConfirmationReasons() {
        CompatibilityResult result = evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                "전구역 동반가능, 사전 문의 필수");
        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasonGroups().confirmation())
                .containsExactly("이용 전 장소 문의가 필요하므로 방문 전 확인해 주세요.");
        assertThat(result.reasonGroups().allowed()).contains(
                "전 구역에서 반려견과 동반할 수 있습니다.");
        assertThat(result.reasons()).doesNotContain("모든 견종의 동반이 허용됩니다.");
        var response = com.petway.place.dto.PlaceCompatibilityResponse.from("test", 1L, "검증견", null, result);
        assertThat(response.petwayAssessment().reasonGroups()).isEqualTo(result.reasonGroups());
    }

    @Test
    void generalPolicyNoticeDoesNotOverrideExplicitAllowanceOrLosePreparations() {
        PetRule rule = parser.parse(List.of(
                new PetRule.PetRuleSource(PetRule.PetRuleSourceField.ACMPY_TYPE_CD, "전구역 동반가능"),
                new PetRule.PetRuleSource(PetRule.PetRuleSourceField.ACMPY_PSBL_CPAM, "모든 견종 동반 가능"),
                new PetRule.PetRuleSource(PetRule.PetRuleSourceField.ETC_ACMPY_INFO,
                        "- 반려견 동반 운영 정책은 사정에 따라 변동 될 수 있음\n- 맹견의 경우, 입마개 착용 필수\n- 배변봉투 지참 및 배변처리 필수")
        ));
        CompatibilityResult result = evaluator.evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), rule);
        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(result.reasonGroups().confirmation()).isEmpty();
        assertThat(result.requirements()).extracting(PetRule.Requirement::type)
                .contains(PetRule.RequirementType.WASTE_BAG, PetRule.RequirementType.WASTE_DISPOSAL)
                .doesNotContain(PetRule.RequirementType.MUZZLE);
        assertThat(rule.sources()).anyMatch(source -> source.rawText().contains("변동 될 수 있음"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"현장 상황에 따라 허용 여부 변경", "중성화 필수",
            "반려견 동반 운영 정책은 사정에 따라 변동 될 수 있음. 사전 문의 필수"})
    void actualRestrictionsStillRequireConfirmation(String text) {
        assertThat(evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), text).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"실내 동반 가능 아님", "실내 출입 금지 아님", "주말 실내 출입 금지",
            "소형견 동반 가능 여부 문의", "모든 견종 동반 가능 아님"})
    void negationAndConditionalRulesAreNotUnconditionalRules(String text) {
        PetRule rule = parser.parse(text);
        assertThat(rule.spaceRules()).isEmpty();
        assertThat(rule.sizeConditions()).isEmpty();
        assertThat(rule.breedRules()).isEmpty();
        assertThat(evaluator.evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), rule).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"대형견 입장 불가", "대형견 출입 금지"})
    void explicitSizeProhibitionOverridesGeneralAllowance(String restriction) {
        assertThat(evaluate(pet(DogBreed.MALTESE, PetSizeType.LARGE), restriction).status())
                .isEqualTo(CompatibilityStatus.UNAVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"맹견 입장 불가", "맹견류 입장 불가", "맹견 출입 금지"})
    void dangerousBreedProhibitionUsesRegisteredBreed(String restriction) {
        assertThat(evaluate(pet(DogBreed.ROTTWEILER, PetSizeType.LARGE), restriction).status())
                .isEqualTo(CompatibilityStatus.UNAVAILABLE);
        assertThat(evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), restriction).status())
                .isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(evaluate(pet(null, PetSizeType.SMALL), restriction).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @ParameterizedTest
    @EnumSource(value = PetRule.PetRuleSourceField.class, names = {
            "UNSPECIFIED", "ACMPY_TYPE_CD", "ACMPY_PSBL_CPAM", "ACMPY_NEED_MTR", "ETC_ACMPY_INFO", "RELA_ACDNT_RISK_MTR"
    })
    void unknownAdmissionRestrictionIsNotIgnoredInAnyDecisionField(PetRule.PetRuleSourceField field) {
        PetRule rule = parser.parse(List.of(
                new PetRule.PetRuleSource(PetRule.PetRuleSourceField.ACMPY_TYPE_CD, "일부구역 동반가능"),
                new PetRule.PetRuleSource(field, "중성화 필수")
        ));
        assertThat(evaluator.evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), rule).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void storeDependentPolicyIsNotAnUnconditionalAllowance() {
        assertThat(evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), "가능(점포마다 다름)").status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void explicitPartialRestrictionIsPreservedAlongsideIndoorAndOutdoorAllowance() {
        CompatibilityResult result = evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                "실내 동반 가능, 실외 동반 가능, 일부구역 동반가능");
        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(result.reasons()).contains("일부 구역에서만 동반할 수 있습니다.")
                .doesNotContain("전 구역에서 반려견과 동반할 수 있습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "실내 동반 가능, 실외 동반 가능",
            "실외 동반 가능, 실내 동반 가능",
            "실내 입장 가능, 야외 입장 가능, 이동장 필수"
    })
    void indoorAndOutdoorTogetherMeanAllAreasAllowed(String text) {
        CompatibilityResult result = evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL), text);
        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(result.reasons()).contains("전 구역에서 반려견과 동반할 수 있습니다.");
    }

    @Test
    void allAreasAllowanceDoesNotOverrideWeightLimit() {
        CompatibilityResult result = evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                "실내 동반 가능, 실외 동반 가능, 반려견 5kg 이하");
        assertThat(result.status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
    }

    @Test
    void conflictingIndoorRuleDoesNotShowAllAreasAllowed() {
        CompatibilityResult result = evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                "실내 동반 가능, 실외 동반 가능, 실내 입장 불가");
        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons()).doesNotContain("전 구역에서 반려견과 동반할 수 있습니다.");
    }

    @Test
    void bothIndoorAndOutdoorProhibitedIsUnavailable() {
        assertThat(evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                "실내 입장 불가, 실외 입장 불가").status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
    }

    @Test
    void catOnlyFacilityDoesNotAllowDogsThroughIndoorFlag() {
        PetFacility facility = facility("고양이");
        PlacePetDetail detail = new PlacePetDetail(facility.normalizedAccompanimentType(),
                facility.normalizedAllowedCapacity(), null, facility.normalizedAdditionalInformation(), null);
        assertThat(evaluator.evaluate(pet(DogBreed.MALTESE, PetSizeType.SMALL),
                parser.parse(new PlacePetRuleSourceAdapter().adapt(detail))).status())
                .isEqualTo(CompatibilityStatus.UNAVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12kg 이하, 금요일은 모두 가능", "개별 문의 필요", "개, 5kg 이하", "5~10kg, 중성화 필수"})
    void normalizationPreservesQualifyingConditions(String capacity) {
        assertThat(facility(capacity).normalizedAllowedCapacity()).isEqualTo(capacity);
    }

    @Test
    void weekdayExceptionRequiresDateInsteadOfUnconditionalWeightRejection() {
        Pet pet = pet(DogBreed.MALTESE, PetSizeType.SMALL);
        ReflectionTestUtils.setField(pet, "weightKg", new BigDecimal("15"));
        PetFacility facility = facility("12kg 이하, 금요일은 모두 가능");
        assertThat(evaluator.evaluate(pet, parser.parse(PetRule.PetRuleSourceField.ACMPY_PSBL_CPAM,
                facility.normalizedAllowedCapacity())).status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"실내 대형견 입장 불가", "13kg 초과 대형견 입장 불가", "주말 맹견 입장 불가", "맹견 입장 불가 아님"})
    void scopedConditionalOrNegatedProhibitionIsNotAWholePlaceBan(String restriction) {
        assertThat(evaluate(pet(DogBreed.ROTTWEILER, PetSizeType.LARGE), restriction).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void inquiryAboutAllBreedsIsNotParsedAsAllBreedsAllowed() {
        assertThat(parser.parse("모든 견종 동반 가능 여부 문의").breedRules()).isEmpty();
    }

    private CompatibilityResult evaluate(Pet pet, String restriction) {
        return evaluator.evaluate(pet, parser.parse(List.of(
                new PetRule.PetRuleSource(PetRule.PetRuleSourceField.ACMPY_PSBL_CPAM, "모든 견종 동반 가능"),
                new PetRule.PetRuleSource(PetRule.PetRuleSourceField.ETC_ACMPY_INFO, restriction))));
    }

    private Pet pet(DogBreed breed, PetSizeType size) {
        Pet pet = Pet.create(null, "검증견", null, null, null, null, new BigDecimal("8"), size,
                null, null, null, null, null, null, false);
        ReflectionTestUtils.setField(pet, "breed", breed);
        return pet;
    }

    private PetFacility facility(String capacity) {
        return new PetFacility("csv-audit", "검증 장소", "반려동반여행", "카페", "서울", "중구",
                null, null, null, null, null, null, null, null, null, null,
                "Y", "해당없음", capacity, null, "Y", "Y", null, null, "2025-03-24");
    }
}
