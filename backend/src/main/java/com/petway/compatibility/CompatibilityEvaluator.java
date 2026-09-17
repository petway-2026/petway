/* 반려견 프로필과 파싱된 규정을 비교하는 규칙 기반 판정기다. AI 호출 없이 사유와 준비사항을 모은다. */
package com.petway.compatibility;

import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.petrule.PetRule;
import com.petway.petrule.PetRule.BreedGroup;
import com.petway.petrule.PetRule.BreedRuleType;
import com.petway.petrule.PetRule.ConfirmationCondition;
import com.petway.petrule.PetRule.FeeRule;
import com.petway.petrule.PetRule.PetRuleSourceField;
import com.petway.petrule.PetRule.PetSize;
import com.petway.petrule.PetRule.Requirement;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SizeCondition;
import com.petway.petrule.PetRule.SizeConditionType;
import com.petway.petrule.PetRule.SpaceCondition;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.petrule.PetRule.UnparsedFragment;
import com.petway.petrule.PetRule.WeightLimit;
import com.petway.petrule.PetRule.WeightOperator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CompatibilityEvaluator {

    // 일반적인 정책 변경 고지만으로 명시된 동반 허용을 확인 필요로 바꾸지 않는다.
    // 날짜·현장별 허용 여부, 전화 문의, 실제 제한은 이 패턴에 포함하지 않는다.
    private static final Pattern GENERAL_POLICY_NOTICE = Pattern.compile(
            "(?:반려견|반려동물)동반(?:운영)?정책[은는]?(?:사정에따라)?"
                    + "(?:변동|변경)될수(?:있음|있습니다|있다)[.!]?"
    );

    // 해석하지 못한 입장 조건은 허용 정보가 다른 필드에 있더라도 무시할 수 없다.
    private static final Pattern UNRESOLVED_ADMISSION_CONDITION = Pattern.compile(
            "불가|금지|제한|필수|필요|문의|확인|여부|경우|조건|따라|예외|점포마다|"
                    + "요일|주말|평일|공휴일|\\d+\\s*(?:kg|㎏)|(?:가능|허용)\\s*(?:아님|하지\\s*않)"
    );

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

        // 공간 범위를 먼저 해석해야 실내·야외에 한정된 체중 조건의 적용 여부를 정할 수 있다.
        Assessment assessment = new Assessment();
        SpaceContext spaceContext = evaluateSpaces(rule, assessment);

        evaluateWeights(pet, rule, spaceContext, assessment);
        evaluateSizes(pet, rule, assessment);
        evaluateBreedRules(pet, rule, assessment);
        evaluateConfirmationConditions(rule, assessment);
        evaluateFees(rule, assessment);
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

        boolean spaceConflict = hasSpaceConflict(conditions);
        if (spaceConflict) {
            assessment.confirm(
                    "공간별 동반 가능 규정이 서로 달라 방문 전 확인이 필요합니다."
            );
        }

        if (!spaceConflict && conditions.contains(SpaceCondition.INDOOR_PROHIBITED)
                && conditions.contains(SpaceCondition.OUTDOOR_PROHIBITED)) {
            assessment.unavailable("실내와 실외 모두 반려견 동반이 금지되어 있습니다.");
            return SpaceContext.UNRESOLVED;
        }

        boolean indoorAndOutdoorAllowed = conditions.contains(SpaceCondition.INDOOR_ALLOWED)
                && conditions.contains(SpaceCondition.OUTDOOR_ALLOWED);
        boolean hasAreaRestriction = conditions.stream().anyMatch(condition -> switch (condition) {
            case INDOOR_PROHIBITED, OUTDOOR_PROHIBITED, OUTDOOR_ONLY,
                    TERRACE_ONLY, PARTIAL_AREA_ONLY -> true;
            default -> false;
        });
        // 실내와 실외를 모두 허용하면 전 구역 허용이다. 명시된 일부 구역 제한은 그대로 우선한다.
        if (indoorAndOutdoorAllowed && !hasAreaRestriction) {
            assessment.positive("전 구역에서 반려견과 동반할 수 있습니다.");
        }

        for (SpaceCondition condition : conditions) {
            if (indoorAndOutdoorAllowed
                    && (condition == SpaceCondition.INDOOR_ALLOWED
                    || condition == SpaceCondition.OUTDOOR_ALLOWED)) {
                continue;
            }
            switch (condition) {
                case ALL_AREAS_ALLOWED -> {
                    if (!hasAreaRestriction) {
                        assessment.positive("전 구역에서 반려견과 동반할 수 있습니다.");
                    }
                }
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
                case OUTDOOR_ONLY -> assessment.positive(
                        "야외에서만 동반할 수 있습니다."
                );
                case TERRACE_ONLY -> assessment.positive(
                        "테라스에서만 동반할 수 있습니다."
                );
                case PARTIAL_AREA_ONLY -> assessment.positive(
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

    // 공간이 불명확한 체중 조건을 장소 전체의 입장 제한으로 확대하지 않고 확인 필요로 남긴다.
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
        if (limit.minWeightKg() != null && petWeight.compareTo(limit.minWeightKg()) < 0) {
            assessment.unavailable(
                    "반려견 체중 " + formatWeight(petWeight) + "kg은 "
                            + formatWeight(limit.minWeightKg()) + "kg 이상 이용 조건을 충족하지 않습니다."
            );
            return;
        }
        // 같은 경계 체중이라도 '이하'는 허용하고 '미만'은 거부하므로 연산자를 구분한다.
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

    // 크기는 사용자가 등록한 sizeType으로 비교한다. 체중에서 크기를 추정하지 않는다.
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

        // 허용과 금지가 동시에 기재된 크기는 어느 한쪽을 임의로 우선하지 않고 확인 대상으로 남긴다.
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
        if (disallowed.contains(petSize)) {
            assessment.unavailable(
                    "장소에서 해당 크기의 반려견 동반을 명시적으로 제한하고 있습니다."
            );
            return;
        }

        if (!allowed.isEmpty() && !allowed.contains(petSize)) {
            assessment.unavailable(
                    "장소에서 등록된 반려견 크기와 다른 크기의 동반을 허용하지 않습니다."
            );
            return;
        }

        if (allowed.contains(petSize) && rule.weightLimits().isEmpty()) {
            assessment.conditional(
                    "등록된 크기는 장소의 허용 표현에 해당하지만, 장소별 크기 기준을 확인해 주세요."
            );
        }
    }

    private void evaluateBreedRules(
            Pet pet,
            PetRule rule,
            Assessment assessment
    ) {
        boolean dangerousBreedsProhibited = rule.breedRules().stream()
                .anyMatch(r -> r.type() == BreedRuleType.DANGEROUS_BREEDS_PROHIBITED);
        if (rule.breedRules().stream().anyMatch(r -> r.type() == BreedRuleType.ALL_BREEDS_ALLOWED)) {
            // 견종 허용은 판정에 반영하되, 모든 입장 조건을 충족했다는 안내로 출력하지 않는다.
            assessment.hasAllowanceEvidence = true;
        }
        if (dangerousBreedsProhibited) {
            if (pet.getBreed() == null) {
                assessment.confirm("맹견 입장 제한이 있어 등록된 견종을 확인해야 합니다.");
            } else if (pet.getBreed().isDangerous()) {
                assessment.unavailable("이 장소는 맹견 동반을 금지하고 있습니다.");
            } else {
                assessment.positive("등록된 견종은 맹견 입장 제한에 해당하지 않습니다.");
            }
        }
    }

    // 견종·크기로 한정된 준비사항은 해당 반려견에게만 적용한다. 대상 정보가 없으면 확인을 요구한다.
    private void evaluateRequirements(
            Pet pet,
            PetRule rule,
            Assessment assessment
    ) {
        for (Requirement requirement : rule.requirements()) {
            if (requirement.target().breedGroup() == BreedGroup.DANGEROUS_BREEDS) {
                if (pet.getBreed() == null) {
                    assessment.confirm(
                            "견종 정보가 없어 맹견 대상 준비사항의 적용 여부를 확인해야 합니다."
                    );
                    continue;
                }
                if (!pet.getBreed().isDangerous()) {
                    continue;
                }
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

            if (requirement.type() == RequirementType.VACCINATION_STATUS) {
                evaluateVaccinationStatus(pet, assessment);
                continue;
            }
            if (requirement.type() == RequirementType.VACCINATION_CERTIFICATE) {
                assessment.require(requirement, requirementReason(requirement));
            } else {
                assessment.requirePreparation(requirement);
            }
        }
    }

    private void evaluateVaccinationStatus(Pet pet, Assessment assessment) {
        if (pet.getVaccinated() == null) {
            assessment.confirm(
                    "장소에서 예방접종을 요구하지만 반려견의 접종 정보가 없어 확인이 필요합니다."
            );
        } else if (pet.getVaccinated()) {
            assessment.positive("반려견의 예방접종 조건을 충족합니다.");
        } else {
            assessment.unavailable("예방접종을 완료한 반려견만 이용할 수 있습니다.");
        }
    }

    private void evaluateConfirmationConditions(
            PetRule rule,
            Assessment assessment
    ) {
        for (ConfirmationCondition condition : rule.confirmationConditions()) {
            String reason = switch (condition.type()) {
                case RESERVATION_REQUIRED ->
                        "이용 전 사전 예약이 필요하므로 방문 전 확인해 주세요.";
                case INQUIRY_REQUIRED ->
                        "이용 전 장소 문의가 필요하므로 방문 전 확인해 주세요.";
            };
            assessment.confirm(reason);
        }
    }

    private void evaluateFees(PetRule rule, Assessment assessment) {
        for (FeeRule feeRule : rule.feeRules()) {
            if (feeRule.amountWon() == null) {
                assessment.conditional(
                        "반려동물 동반 시 추가 요금이 발생하므로 금액을 확인해 주세요."
                );
                continue;
            }
            String amount = NumberFormat.getIntegerInstance(Locale.KOREA)
                    .format(feeRule.amountWon());
            assessment.conditional("반려동물 동반 시 추가 요금 " + amount + "원이 필요합니다.");
        }
    }

    private void evaluateUnparsedFragments(
            PetRule rule,
            Assessment assessment
    ) {
        // 기존에 얻은 판정 근거와 원문 출처에 따라 미해석 문장이 최종 확인 필요 상태에 영향을 줄지 정한다.
        boolean hasConcreteOutcome = assessment.hasOutcomeEvidence();
        for (UnparsedFragment fragment : rule.unparsedFragments()) {
            if (assessment.hasAllowanceEvidence
                    && fragment.reason() != PetRule.UnparsedReason.PARSER_ERROR
                    && fragment.sourceField() == PetRuleSourceField.ETC_ACMPY_INFO
                    && GENERAL_POLICY_NOTICE.matcher(fragment.rawText().replaceAll("\\s+", "")).matches()) {
                continue;
            }
            if (isDecisionRelevant(fragment)
                    && (!hasConcreteOutcome
                    || fragment.reason() == PetRule.UnparsedReason.PARSER_ERROR
                    || fragment.sourceField() == PetRuleSourceField.ACMPY_PSBL_CPAM
                    || UNRESOLVED_ADMISSION_CONDITION.matcher(fragment.rawText()).find())) {
                assessment.confirm(unparsedReason(fragment.sourceField())
                        + " 원문: “" + fragment.rawText().trim() + "”");
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
            case VACCINATION_STATUS -> "예방접종 완료가 필요합니다.";
            case VACCINATION_CERTIFICATE -> "예방접종 증명서를 준비해야 합니다.";
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
        private boolean hasAllowanceEvidence;

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
            hasAllowanceEvidence = true;
            positiveReasons.add(reason);
        }

        private void require(Requirement requirement, String reason) {
            requirements.add(requirement);
            conditional(reason);
        }

        // 현재 정책에서는 일반 준비물은 목록에만 추가한다. 이것만으로 CONDITIONAL 상태를 만들지는 않는다.
        private void requirePreparation(Requirement requirement) {
            requirements.add(requirement);
        }

        private boolean hasOutcomeEvidence() {
            return !unavailableReasons.isEmpty()
                    || !confirmationReasons.isEmpty()
                    || !conditionalReasons.isEmpty()
                    || hasAllowanceEvidence
                    || !requirements.isEmpty();
        }

        // 최종 우선순위는 명확한 불가 → 확인 필요 → 조건부 → 가능이다.
        // 여러 조건을 모두 평가한 근거는 상태와 별도로 함께 반환한다.
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
            return new CompatibilityResult(status, reasons, requirements,
                    new CompatibilityResult.ReasonGroups(
                            List.copyOf(unavailableReasons), List.copyOf(confirmationReasons),
                            List.copyOf(conditionalReasons), List.copyOf(positiveReasons)));
        }
    }
}
