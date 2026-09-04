package com.petway.compatibility;

import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.petrule.PetRule;
import com.petway.petrule.PetRule.BreedRule;
import com.petway.petrule.PetRule.BreedRuleType;
import com.petway.petrule.PetRule.PetRuleSource;
import com.petway.petrule.PetRule.PetRuleSourceField;
import com.petway.petrule.PetRule.PetSize;
import com.petway.petrule.PetRule.PetSubject;
import com.petway.petrule.PetRule.Requirement;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SizeCondition;
import com.petway.petrule.PetRule.SizeConditionType;
import com.petway.petrule.PetRule.SpaceCondition;
import com.petway.petrule.PetRule.SpaceRule;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.petrule.PetRule.UnparsedFragment;
import com.petway.petrule.PetRule.UnparsedReason;
import com.petway.petrule.PetRule.WeightLimit;
import com.petway.petrule.PetRule.WeightOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityEvaluatorTest {

    private CompatibilityEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CompatibilityEvaluator();
    }

    @Test
    void acceptsWeightEqualToLessThanOrEqualLimit() {
        CompatibilityResult result = evaluate(
                pet("20", PetSizeType.LARGE),
                ruleWithWeight("20", WeightOperator.LESS_THAN_OR_EQUAL)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(result.reasons()).contains("반려견의 체중이 이용 기준을 충족합니다.");
    }

    @Test
    void rejectsWeightEqualToStrictLessThanLimit() {
        CompatibilityResult result = evaluate(
                pet("20", PetSizeType.SMALL),
                ruleWithWeight("20", WeightOperator.LESS_THAN)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
        assertThat(result.reasons()).contains(
                "반려견 체중 20kg은 20kg 미만 이용 조건을 충족하지 않습니다."
        );
    }

    @Test
    void acceptsDecimalWeightBelowStrictLimit() {
        CompatibilityResult result = evaluate(
                pet("19.9", PetSizeType.LARGE),
                ruleWithWeight("20", WeightOperator.LESS_THAN)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
    }

    @Test
    void rejectsWeightBelowMinimumWithReadableMessage() {
        PetRule rule = rule(
                List.of(new WeightLimit(
                        new BigDecimal("10"),
                        WeightOperator.LESS_THAN_OR_EQUAL,
                        SpaceScope.UNSPECIFIED,
                        source(PetRuleSourceField.ETC_ACMPY_INFO),
                        new BigDecimal("5")
                )),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );

        CompatibilityResult result = evaluate(pet("4.5", PetSizeType.SMALL), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
        assertThat(result.reasons()).contains(
                "반려견 체중 4.5kg은 5kg 이상 이용 조건을 충족하지 않습니다."
        );
    }

    @Test
    void needsConfirmationWhenPetWeightIsMissing() {
        CompatibilityResult result = evaluate(
                pet(null, PetSizeType.SMALL),
                ruleWithWeight("20", WeightOperator.LESS_THAN)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons()).contains(
                "반려견의 체중 정보가 없어 이용 조건을 확인할 수 없습니다."
        );
    }

    @Test
    void doesNotTreatIndoorAndOutdoorLimitsAsGlobal() {
        PetRule rule = rule(
                List.of(
                        weight("5", WeightOperator.LESS_THAN_OR_EQUAL, SpaceScope.INDOOR),
                        weight("20", WeightOperator.LESS_THAN, SpaceScope.OUTDOOR)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        CompatibilityResult result = evaluate(pet("10", PetSizeType.MEDIUM), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons())
                .contains("특정 공간에 적용되는 체중 조건은 방문 전 확인이 필요합니다.")
                .noneMatch(reason -> reason.contains("충족하지 않습니다"));
    }

    @Test
    void appliesOnlyOutdoorWeightWhenOutdoorIsTheOnlyUsableSpace() {
        PetRule rule = rule(
                List.of(
                        weight("5", WeightOperator.LESS_THAN_OR_EQUAL, SpaceScope.INDOOR),
                        weight("20", WeightOperator.LESS_THAN, SpaceScope.OUTDOOR)
                ),
                List.of(),
                List.of(space(SpaceCondition.OUTDOOR_ONLY)),
                List.of(),
                List.of(),
                List.of()
        );

        CompatibilityResult result = evaluate(pet("10", PetSizeType.MEDIUM), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.reasons())
                .contains("야외에서만 동반할 수 있습니다.")
                .noneMatch(reason -> reason.contains("충족하지 않습니다"));
    }

    @Test
    void rejectsOutdoorWeightWhenOutdoorIsTheOnlyUsableSpace() {
        PetRule rule = rule(
                List.of(weight("20", WeightOperator.LESS_THAN, SpaceScope.OUTDOOR)),
                List.of(),
                List.of(space(SpaceCondition.OUTDOOR_ONLY)),
                List.of(),
                List.of(),
                List.of()
        );

        CompatibilityResult result = evaluate(pet("25", PetSizeType.MEDIUM), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
        assertThat(result.reasons()).contains(
                "반려견 체중 25kg은 20kg 미만 이용 조건을 충족하지 않습니다."
        );
    }

    @Test
    void doesNotApplyOutdoorWeightToTerraceOnlySpace() {
        PetRule rule = rule(
                List.of(weight("20", WeightOperator.LESS_THAN, SpaceScope.OUTDOOR)),
                List.of(),
                List.of(space(SpaceCondition.TERRACE_ONLY)),
                List.of(),
                List.of(),
                List.of()
        );

        CompatibilityResult result = evaluate(pet("25", PetSizeType.MEDIUM), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons())
                .contains("특정 공간에 적용되는 체중 조건은 방문 전 확인이 필요합니다.")
                .noneMatch(reason -> reason.contains("충족하지 않습니다"));
    }

    @Test
    void oneScopedLimitCannotRejectTheWholePlace() {
        PetRule rule = rule(
                List.of(weight("5", WeightOperator.LESS_THAN_OR_EQUAL, SpaceScope.INDOOR)),
                List.of(),
                List.of(space(SpaceCondition.ALL_AREAS_ALLOWED)),
                List.of(),
                List.of(),
                List.of()
        );

        CompatibilityResult result = evaluate(pet("10", PetSizeType.MEDIUM), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons()).noneMatch(reason -> reason.contains("충족하지 않습니다"));
    }

    @Test
    void treatsMatchingAllowedSizeAsConditionalBecauseVenueThresholdIsUnknown() {
        CompatibilityResult result = evaluate(
                pet("20", PetSizeType.SMALL),
                ruleWithSize(Set.of(PetSize.SMALL), SizeConditionType.ALLOWED)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.reasons()).contains(
                "등록된 크기는 장소의 허용 표현에 해당하지만, 장소별 크기 기준을 확인해 주세요."
        );
    }

    @Test
    void needsConfirmationForSizeOutsideAllowedLabel() {
        CompatibilityResult result = evaluate(
                pet("2", PetSizeType.LARGE),
                ruleWithSize(Set.of(PetSize.SMALL), SizeConditionType.ALLOWED)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons()).contains(
                "장소의 반려견 크기 기준이 명확하지 않아 방문 전 확인이 필요합니다."
        );
    }

    @Test
    void rejectsExplicitlyDisallowedSize() {
        CompatibilityResult result = evaluate(
                pet("20", PetSizeType.LARGE),
                ruleWithSize(Set.of(PetSize.LARGE), SizeConditionType.DISALLOWED)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
        assertThat(result.reasons()).contains(
                "장소에서 해당 크기의 반려견 동반을 명시적으로 제한하고 있습니다."
        );
    }

    @Test
    void needsConfirmationWhenSizeIsMissing() {
        CompatibilityResult result = evaluate(
                pet("10", null),
                ruleWithSize(Set.of(PetSize.SMALL), SizeConditionType.ALLOWED)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void neverInfersSizeFromLowWeight() {
        CompatibilityResult result = evaluate(
                pet("2", null),
                ruleWithSize(Set.of(PetSize.SMALL), SizeConditionType.ALLOWED)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.reasons()).contains(
                "반려견의 크기 정보가 없어 이용 조건을 확인할 수 없습니다."
        );
    }

    @Test
    void conflictingSizeRulesNeedConfirmation() {
        PetRule rule = rule(
                List.of(),
                List.of(
                        size(Set.of(PetSize.SMALL), SizeConditionType.ALLOWED),
                        size(Set.of(PetSize.SMALL), SizeConditionType.DISALLOWED)
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );

        CompatibilityResult result = evaluate(pet("5", PetSizeType.SMALL), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void leashRequirementIsConditional() {
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithRequirements(requirement(RequirementType.LEASH, PetSubject.unspecified()))
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.reasons()).contains("목줄 착용이 필요합니다.");
        assertThat(result.requirements()).hasSize(1);
    }

    @Test
    void carrierRequirementIsConditional() {
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithRequirements(requirement(RequirementType.CARRIER, PetSubject.unspecified()))
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.reasons()).contains("이동장 사용이 필요합니다.");
    }

    @Test
    void preservesMultipleRequirementsAndReasons() {
        Requirement leash = requirement(RequirementType.LEASH, PetSubject.unspecified());
        Requirement wasteBag = requirement(RequirementType.WASTE_BAG, PetSubject.unspecified());
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithRequirements(leash, wasteBag)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.requirements()).containsExactly(leash, wasteBag);
        assertThat(result.reasons())
                .contains("목줄 착용이 필요합니다.", "배변봉투를 준비해야 합니다.");
    }

    @Test
    void unknownDangerousBreedTargetNeedsConfirmation() {
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithRequirements(requirement(
                        RequirementType.MUZZLE,
                        PetSubject.dangerousBreeds()
                ))
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(result.requirements()).isEmpty();
    }

    @Test
    void allAreasAllowedCanBeAvailable() {
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithSpace(SpaceCondition.ALL_AREAS_ALLOWED)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.AVAILABLE);
    }

    @Test
    void partialAreaIsConditional() {
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithSpace(SpaceCondition.PARTIAL_AREA_ONLY)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
    }

    @Test
    void outdoorOnlyIsConditional() {
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithSpace(SpaceCondition.OUTDOOR_ONLY)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
    }

    @Test
    void terraceOnlyWithoutOtherUncertaintyIsConditional() {
        CompatibilityResult result = evaluate(
                pet("5", PetSizeType.SMALL),
                ruleWithSpace(SpaceCondition.TERRACE_ONLY)
        );

        assertThat(result.status()).isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(result.reasons()).contains("테라스에서만 동반할 수 있습니다.");
    }

    @Test
    void decisionRelevantUnparsedFragmentNeedsConfirmation() {
        PetRule rule = rule(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(allBreedsAllowed()),
                List.of(unparsed(PetRuleSourceField.ETC_ACMPY_INFO))
        );

        assertThat(evaluate(pet("5", PetSizeType.SMALL), rule).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void informationalUnparsedFragmentDoesNotRaiseAvailableResult() {
        PetRule rule = rule(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(allBreedsAllowed()),
                List.of(unparsed(PetRuleSourceField.RELA_PURC_PRDLST))
        );

        assertThat(evaluate(pet("5", PetSizeType.SMALL), rule).status())
                .isEqualTo(CompatibilityStatus.AVAILABLE);
    }

    @Test
    void noDecisionInformationNeedsConfirmation() {
        assertThat(evaluate(pet("5", PetSizeType.SMALL), PetRule.empty()).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void unavailableHasPriorityOverRequirement() {
        PetRule rule = rule(
                List.of(weight("5", WeightOperator.LESS_THAN, SpaceScope.UNSPECIFIED)),
                List.of(),
                List.of(),
                List.of(requirement(RequirementType.LEASH, PetSubject.unspecified())),
                List.of(),
                List.of()
        );

        CompatibilityResult result = evaluate(pet("10", PetSizeType.SMALL), rule);

        assertThat(result.status()).isEqualTo(CompatibilityStatus.UNAVAILABLE);
        assertThat(result.requirements()).hasSize(1);
    }

    @Test
    void confirmationHasPriorityOverRequirement() {
        PetRule rule = rule(
                List.of(),
                List.of(size(Set.of(PetSize.SMALL), SizeConditionType.ALLOWED)),
                List.of(),
                List.of(requirement(RequirementType.LEASH, PetSubject.unspecified())),
                List.of(),
                List.of()
        );

        assertThat(evaluate(pet("10", null), rule).status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
    }

    @Test
    void clearAllowedWithRequirementIsConditional() {
        PetRule rule = rule(
                List.of(),
                List.of(),
                List.of(space(SpaceCondition.ALL_AREAS_ALLOWED)),
                List.of(requirement(RequirementType.LEASH, PetSubject.unspecified())),
                List.of(),
                List.of()
        );

        assertThat(evaluate(pet("5", PetSizeType.SMALL), rule).status())
                .isEqualTo(CompatibilityStatus.CONDITIONAL);
    }

    @Test
    void clearAllowedOnlyIsAvailable() {
        PetRule rule = rule(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(allBreedsAllowed()),
                List.of()
        );

        assertThat(evaluate(pet("5", PetSizeType.SMALL), rule).status())
                .isEqualTo(CompatibilityStatus.AVAILABLE);
    }

    private CompatibilityResult evaluate(Pet pet, PetRule rule) {
        return evaluator.evaluate(pet, rule);
    }

    private Pet pet(String weightKg, PetSizeType sizeType) {
        return Pet.create(
                null,
                "두부",
                null,
                null,
                null,
                null,
                weightKg == null ? null : new BigDecimal(weightKg),
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

    private PetRule ruleWithWeight(String maxWeight, WeightOperator operator) {
        return rule(
                List.of(weight(maxWeight, operator, SpaceScope.UNSPECIFIED)),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    private PetRule ruleWithSize(Set<PetSize> sizes, SizeConditionType type) {
        return rule(
                List.of(), List.of(size(sizes, type)), List.of(), List.of(), List.of(), List.of()
        );
    }

    private PetRule ruleWithRequirements(Requirement... requirements) {
        return rule(
                List.of(), List.of(), List.of(), List.of(requirements), List.of(), List.of()
        );
    }

    private PetRule ruleWithSpace(SpaceCondition condition) {
        return rule(
                List.of(), List.of(), List.of(space(condition)), List.of(), List.of(), List.of()
        );
    }

    private PetRule rule(
            List<WeightLimit> weights,
            List<SizeCondition> sizes,
            List<SpaceRule> spaces,
            List<Requirement> requirements,
            List<BreedRule> breeds,
            List<UnparsedFragment> unparsed
    ) {
        return new PetRule(
                List.of(), weights, sizes, spaces, requirements, breeds, unparsed
        );
    }

    private WeightLimit weight(
            String maxWeight,
            WeightOperator operator,
            SpaceScope scope
    ) {
        return new WeightLimit(
                new BigDecimal(maxWeight), operator, scope, source(PetRuleSourceField.ETC_ACMPY_INFO)
        );
    }

    private SizeCondition size(Set<PetSize> sizes, SizeConditionType type) {
        return new SizeCondition(sizes, type, source(PetRuleSourceField.ACMPY_PSBL_CPAM));
    }

    private SpaceRule space(SpaceCondition condition) {
        return new SpaceRule(condition, source(PetRuleSourceField.ACMPY_TYPE_CD));
    }

    private Requirement requirement(RequirementType type, PetSubject target) {
        return new Requirement(
                type,
                SpaceScope.UNSPECIFIED,
                target,
                source(PetRuleSourceField.ACMPY_NEED_MTR)
        );
    }

    private BreedRule allBreedsAllowed() {
        return new BreedRule(
                BreedRuleType.ALL_BREEDS_ALLOWED,
                source(PetRuleSourceField.ACMPY_PSBL_CPAM)
        );
    }

    private UnparsedFragment unparsed(PetRuleSourceField sourceField) {
        return new UnparsedFragment(
                sourceField,
                "원문 정보",
                UnparsedReason.UNSUPPORTED_CONTENT
        );
    }

    private PetRuleSource source(PetRuleSourceField sourceField) {
        return new PetRuleSource(sourceField, "테스트 원문");
    }
}
