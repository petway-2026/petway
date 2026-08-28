package com.petway.compatibility;

import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.petrule.PetRule;
import com.petway.petrule.PetRule.BreedGroup;
import com.petway.petrule.PetRule.PetRuleSourceField;
import com.petway.petrule.PetRule.PetSize;
import com.petway.petrule.PetRule.Requirement;
import com.petway.petrule.PetRule.SizeCondition;
import com.petway.petrule.PetRule.SizeConditionType;
import com.petway.petrule.PetRule.SpaceCondition;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.petrule.PetRule.UnparsedFragment;
import com.petway.petrule.PetRule.WeightLimit;
import com.petway.petrule.PetRule.WeightOperator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class CompatibilityEvaluator {

    private static final Set<PetRuleSourceField> DECISION_RELEVANT_SOURCES = EnumSet.of(
            PetRuleSourceField.UNSPECIFIED,
            PetRuleSourceField.ACMPY_TYPE_CD,
            PetRuleSourceField.ACMPY_PSBL_CPAM,
            PetRuleSourceField.ACMPY_NEED_MTR,
            PetRuleSourceField.ETC_ACMPY_INFO,
            PetRuleSourceField.RELA_ACDNT_RISK_MTR
    );

    public CompatibilityResult evaluate(Pet pet, PetRule rule) {
        Objects.requireNonNull(pet, "pet must not be null");
        Objects.requireNonNull(rule, "rule must not be null");

        Assessment assessment = new Assessment();
        SpaceContext spaceContext = evaluateSpaces(rule, assessment);

        evaluateWeights(pet, rule, spaceContext, assessment);
        evaluateSizes(pet, rule, assessment);
        evaluateBreedRules(rule, assessment);
        evaluateRequirements(pet, rule, assessment);
        evaluateUnparsedFragments(rule, assessment);

        if (!rule.hasParsedCondition()
                && rule.unparsedFragments().stream()
                .noneMatch(this::isDecisionRelevant)) {
            assessment.confirm(
                    "이용 가능 여부를 판단할 수 있는 반려동물 동반 규정이 없습니다."
            );
        } else if (!assessment.hasOutcomeEvidence()) {
            assessment.confirm(
                    "동반 가능 여부를 확정할 명확한 허용 정보가 없습니다."
            );
        }

        return assessment.toResult();
    }

    private SpaceContext evaluateSpaces(
            PetRule rule,
            Assessment assessment
    ) {
        Set<SpaceCondition> conditions = EnumSet.noneOf(SpaceCondition.class);
        rule.spaceRules().forEach(spaceRule -> conditions.add(spaceRule.condition()));

        if (hasSpaceConflict(conditions)) {
            assessment.confirm(
                    "공간별 동반 가능 규정이 서로 달라 방문 전 확인이 필요합니다."
            );
        }

        for (SpaceCondition condition : conditions) {
            switch (condition) {
                case ALL_AREAS_ALLOWED -> assessment.positive(
                        "전 구역에서 반려견과 동반할 수 있습니다."
                );
                case INDOOR_ALLOWED -> assessment.positive(
                        "실내 동반이 허용됩니다."
                );
                case OUTDOOR_ALLOWED -> assessment.positive(
                        "야외 동반이 허용됩니다."
                );
                case INDOOR_PROHIBITED -> assessment.conditional(
                        "실내에서는 동반할 수 없습니다."
                );
                case OUTDOOR_PROHIBITED -> assessment.conditional(
                        "야외에서는 동반할 수 없습니다."
                );
                case OUTDOOR_ONLY -> assessment.conditional(
                        "야외에서만 동반할 수 있습니다."
                );
                case TERRACE_ONLY -> assessment.conditional(
                        "테라스에서만 동반할 수 있습니다."
                );
                case PARTIAL_AREA_ONLY -> assessment.conditional(
                        "일부 구역에서만 동반할 수 있습니다."
                );
            }
        }

        boolean outdoorOnly = conditions.contains(SpaceCondition.OUTDOOR_ONLY);
        boolean outdoorConflict = conditions.contains(SpaceCondition.ALL_AREAS_ALLOWED)
                || conditions.contains(SpaceCondition.INDOOR_ALLOWED)
                || conditions.contains(SpaceCondition.OUTDOOR_PROHIBITED);

        return outdoorOnly && !outdoorConflict
                ? SpaceContext.OUTDOOR_ONLY
                : SpaceContext.UNRESOLVED;
    }

    private boolean hasSpaceConflict(Set<SpaceCondition> conditions) {
        boolean allAreasConflict = conditions.contains(SpaceCondition.ALL_AREAS_ALLOWED)
                && conditions.stream().anyMatch(condition -> switch (condition) {
                    case INDOOR_PROHIBITED, OUTDOOR_PROHIBITED, OUTDOOR_ONLY,
                            TERRACE_ONLY, PARTIAL_AREA_ONLY -> true;
                    default -> false;
                });
        boolean indoorConflict = conditions.contains(SpaceCondition.INDOOR_ALLOWED)
                && conditions.contains(SpaceCondition.INDOOR_PROHIBITED);
        boolean outdoorConflict = conditions.contains(SpaceCondition.OUTDOOR_ALLOWED)
                && conditions.contains(SpaceCondition.OUTDOOR_PROHIBITED);
        boolean outdoorOnlyConflict = conditions.contains(SpaceCondition.OUTDOOR_ONLY)
                && (conditions.contains(SpaceCondition.INDOOR_ALLOWED)
                || conditions.contains(SpaceCondition.OUTDOOR_PROHIBITED));

        return allAreasConflict || indoorConflict || outdoorConflict || outdoorOnlyConflict;
    }

    private void evaluateWeights(
            Pet pet,
            PetRule rule,
            SpaceContext spaceContext,
            Assessment assessment
    ) {
        if (rule.weightLimits().isEmpty()) {
            return;
        }
        if (pet.getWeightKg() == null) {
            assessment.confirm(
                    "반려견의 체중 정보가 없어 이용 조건을 확인할 수 없습니다."
            );
            return;
        }

        for (WeightLimit limit : rule.weightLimits()) {
            if (limit.scope() == SpaceScope.UNSPECIFIED) {
                evaluateApplicableWeight(pet.getWeightKg(), limit, assessment);
            } else if (spaceContext == SpaceContext.OUTDOOR_ONLY
                    && limit.scope() == SpaceScope.OUTDOOR) {
                evaluateApplicableWeight(pet.getWeightKg(), limit, assessment);
            } else if (!(spaceContext == SpaceContext.OUTDOOR_ONLY
                    && limit.scope() == SpaceScope.INDOOR)) {
                assessment.confirm(
                        "특정 공간에 적용되는 체중 조건은 방문 전 확인이 필요합니다."
                );
            }
        }
    }

    private void evaluateApplicableWeight(
            BigDecimal petWeight,
            WeightLimit limit,
            Assessment assessment
    ) {
        int comparison = petWeight.compareTo(limit.maxWeightKg());
        boolean matches = limit.operator() == WeightOperator.LESS_THAN_OR_EQUAL
                ? comparison <= 0
                : comparison < 0;

        if (matches) {
            assessment.positive(
                    "반려견의 체중이 이용 기준을 충족합니다."
            );
            return;
        }

        assessment.unavailable(
                "반려견 체중 " + formatWeight(petWeight)
                        + "kg은 " + formatWeight(limit.maxWeightKg()) + "kg "
                        + operatorText(limit.operator())
                        + " 이용 조건을 충족하지 않습니다."
        );
    }

    private void evaluateSizes(
            Pet pet,
            PetRule rule,
            Assessment assessment
    ) {
        if (rule.sizeConditions().isEmpty()) {
            return;
        }
        if (pet.getSizeType() == null) {
            assessment.confirm(
                    "반려견의 크기 정보가 없어 이용 조건을 확인할 수 없습니다."
            );
            return;
        }

        Set<PetSize> allowed = EnumSet.noneOf(PetSize.class);
        Set<PetSize> disallowed = EnumSet.noneOf(PetSize.class);
        for (SizeCondition condition : rule.sizeConditions()) {
            if (condition.type() == SizeConditionType.ALLOWED) {
                allowed.addAll(condition.sizes());
            } else {
                disallowed.addAll(condition.sizes());
            }
        }

        Set<PetSize> conflicts = EnumSet.noneOf(PetSize.class);
        conflicts.addAll(allowed);
        conflicts.retainAll(disallowed);
        if (!conflicts.isEmpty()) {
            assessment.confirm(
                    "반려견 크기 규정이 서로 충돌하여 방문 전 확인이 필요합니다."
            );
            return;
        }

        PetSize petSize = toRuleSize(pet.getSizeType());
        if ((!allowed.isEmpty() && !allowed.contains(petSize))
                || disallowed.contains(petSize)) {
            assessment.unavailable(
                    "반려견의 크기가 허용된 이용 조건에 해당하지 않습니다."
            );
        } else if (allowed.contains(petSize)) {
            assessment.positive(
                    "반려견의 크기가 이용 기준을 충족합니다."
            );
        }
    }

    private void evaluateBreedRules(
            PetRule rule,
            Assessment assessment
    ) {
        if (!rule.breedRules().isEmpty()) {
            assessment.positive(
                    "모든 견종의 동반이 허용됩니다."
            );
        }
    }

    private void evaluateRequirements(
            Pet pet,
            PetRule rule,
            Assessment assessment
    ) {
        for (Requirement requirement : rule.requirements()) {
            if (requirement.target().breedGroup() == BreedGroup.DANGEROUS_BREEDS) {
                assessment.confirm(
                        "대상 견종에만 적용되는 준비사항은 방문 전 확인이 필요합니다."
                );
                continue;
            }

            if (!requirement.target().sizes().isEmpty()) {
                if (pet.getSizeType() == null) {
                    assessment.confirm(
                            "크기별 준비사항의 적용 여부를 확인하려면 반려견 크기 정보가 필요합니다."
                    );
                    continue;
                }
                if (!requirement.target().sizes().contains(toRuleSize(pet.getSizeType()))) {
                    continue;
                }
            }

            assessment.require(requirement, requirementReason(requirement));
        }
    }

    private void evaluateUnparsedFragments(
            PetRule rule,
            Assessment assessment
    ) {
        for (UnparsedFragment fragment : rule.unparsedFragments()) {
            if (isDecisionRelevant(fragment)) {
                assessment.confirm(unparsedReason(fragment.sourceField()));
            }
        }
    }

    private boolean isDecisionRelevant(UnparsedFragment fragment) {
        return DECISION_RELEVANT_SOURCES.contains(fragment.sourceField());
    }

    private String unparsedReason(PetRuleSourceField sourceField) {
        return switch (sourceField) {
            case ACMPY_TYPE_CD ->
                    "동반 가능 범위에 확인되지 않은 내용이 있어 방문 전 확인이 필요합니다.";
            case ACMPY_PSBL_CPAM ->
                    "동반 가능한 반려동물 조건에 확인되지 않은 내용이 있습니다.";
            case ACMPY_NEED_MTR ->
                    "필요 준비사항에 확인되지 않은 내용이 있어 방문 전 확인이 필요합니다.";
            case ETC_ACMPY_INFO ->
                    "기타 동반 규정에 확인되지 않은 내용이 있어 방문 전 확인이 필요합니다.";
            case RELA_ACDNT_RISK_MTR ->
                    "사고 대비 안내에 확인이 필요한 내용이 있습니다.";
            default ->
                    "동반 규정에 확인되지 않은 내용이 있어 방문 전 확인이 필요합니다.";
        };
    }

    private String requirementReason(Requirement requirement) {
        String baseReason = switch (requirement.type()) {
            case LEASH -> "목줄 착용이 필요합니다.";
            case MUZZLE -> "입마개 착용이 필요합니다.";
            case WASTE_BAG -> "배변봉투를 준비해야 합니다.";
            case WASTE_DISPOSAL -> "배변 처리가 필요합니다.";
            case CARRIER -> "이동장 사용이 필요합니다.";
            case STROLLER -> "유모차 사용이 필요합니다.";
            case MANNER_BELT -> "매너벨트 착용이 필요합니다.";
        };

        return switch (requirement.scope()) {
            case INDOOR -> "실내 이용 시 " + baseReason;
            case OUTDOOR -> "야외 이용 시 " + baseReason;
            case UNSPECIFIED -> baseReason;
        };
    }

    private PetSize toRuleSize(PetSizeType sizeType) {
        return switch (sizeType) {
            case SMALL -> PetSize.SMALL;
            case MEDIUM -> PetSize.MEDIUM;
            case LARGE -> PetSize.LARGE;
        };
    }

    private String operatorText(WeightOperator operator) {
        return operator == WeightOperator.LESS_THAN_OR_EQUAL ? "이하" : "미만";
    }

    private String formatWeight(BigDecimal weight) {
        return weight.stripTrailingZeros().toPlainString();
    }

    private enum SpaceContext {
        OUTDOOR_ONLY,
        UNRESOLVED
    }

    private static final class Assessment {

        private final Set<String> unavailableReasons = new LinkedHashSet<>();
        private final Set<String> confirmationReasons = new LinkedHashSet<>();
        private final Set<String> conditionalReasons = new LinkedHashSet<>();
        private final Set<String> positiveReasons = new LinkedHashSet<>();
        private final List<Requirement> requirements = new ArrayList<>();

        private void unavailable(String reason) {
            unavailableReasons.add(reason);
        }

        private void confirm(String reason) {
            confirmationReasons.add(reason);
        }

        private void conditional(String reason) {
            conditionalReasons.add(reason);
        }

        private void positive(String reason) {
            positiveReasons.add(reason);
        }

        private void require(Requirement requirement, String reason) {
            requirements.add(requirement);
            conditional(reason);
        }

        private boolean hasOutcomeEvidence() {
            return !unavailableReasons.isEmpty()
                    || !confirmationReasons.isEmpty()
                    || !conditionalReasons.isEmpty()
                    || !positiveReasons.isEmpty();
        }

        private CompatibilityResult toResult() {
            CompatibilityStatus status;
            if (!unavailableReasons.isEmpty()) {
                status = CompatibilityStatus.UNAVAILABLE;
            } else if (!confirmationReasons.isEmpty()) {
                status = CompatibilityStatus.NEED_CONFIRMATION;
            } else if (!conditionalReasons.isEmpty()) {
                status = CompatibilityStatus.CONDITIONAL;
            } else {
                status = CompatibilityStatus.AVAILABLE;
            }

            List<String> reasons = new ArrayList<>();
            reasons.addAll(unavailableReasons);
            reasons.addAll(confirmationReasons);
            reasons.addAll(conditionalReasons);
            reasons.addAll(positiveReasons);
            return new CompatibilityResult(status, reasons, requirements);
        }
    }
}
