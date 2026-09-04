package com.petway.petrule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PetRule(
        List<PetRuleSource> sources,
        List<WeightLimit> weightLimits,
        List<SizeCondition> sizeConditions,
        List<SpaceRule> spaceRules,
        List<Requirement> requirements,
        List<BreedRule> breedRules,
        List<ConfirmationCondition> confirmationConditions,
        List<FeeRule> feeRules,
        List<UnparsedFragment> unparsedFragments
) {

    public PetRule {
        sources = immutableList(sources);
        weightLimits = immutableList(weightLimits);
        sizeConditions = immutableList(sizeConditions);
        spaceRules = immutableList(spaceRules);
        requirements = immutableList(requirements);
        breedRules = immutableList(breedRules);
        confirmationConditions = immutableList(confirmationConditions);
        feeRules = immutableList(feeRules);
        unparsedFragments = immutableList(unparsedFragments);
    }

    public PetRule(
            List<PetRuleSource> sources,
            List<WeightLimit> weightLimits,
            List<SizeCondition> sizeConditions,
            List<SpaceRule> spaceRules,
            List<Requirement> requirements,
            List<BreedRule> breedRules,
            List<ConfirmationCondition> confirmationConditions,
            List<UnparsedFragment> unparsedFragments
    ) {
        this(sources, weightLimits, sizeConditions, spaceRules, requirements,
                breedRules, confirmationConditions, List.of(), unparsedFragments);
    }

    public PetRule(
            List<PetRuleSource> sources,
            List<WeightLimit> weightLimits,
            List<SizeCondition> sizeConditions,
            List<SpaceRule> spaceRules,
            List<Requirement> requirements,
            List<BreedRule> breedRules,
            List<UnparsedFragment> unparsedFragments
    ) {
        this(sources, weightLimits, sizeConditions, spaceRules, requirements,
                breedRules, List.of(), List.of(), unparsedFragments);
    }

    public static PetRule empty() {
        return new PetRule(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    public boolean hasParsedCondition() {
        return !weightLimits.isEmpty()
                || !sizeConditions.isEmpty()
                || !spaceRules.isEmpty()
                || !requirements.isEmpty()
                || !breedRules.isEmpty()
                || !confirmationConditions.isEmpty()
                || !feeRules.isEmpty();
    }

    public boolean hasUnknownContent() {
        return !unparsedFragments.isEmpty();
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null || values.isEmpty()
                ? List.of()
                : List.copyOf(values);
    }

    public record PetRuleSource(
            PetRuleSourceField sourceField,
            String rawText
    ) {

        public PetRuleSource {
            Objects.requireNonNull(sourceField, "sourceField must not be null");
        }
    }

    public record WeightLimit(
            BigDecimal maxWeightKg,
            WeightOperator operator,
            SpaceScope scope,
            PetRuleSource source,
            BigDecimal minWeightKg
    ) {

        public WeightLimit(BigDecimal maxWeightKg, WeightOperator operator, SpaceScope scope, PetRuleSource source) {
            this(maxWeightKg, operator, scope, source, null);
        }

        public WeightLimit {
            Objects.requireNonNull(maxWeightKg, "maxWeightKg must not be null");
            Objects.requireNonNull(operator, "operator must not be null");
            Objects.requireNonNull(scope, "scope must not be null");
            Objects.requireNonNull(source, "source must not be null");

            if (maxWeightKg.signum() < 0) {
                throw new IllegalArgumentException("maxWeightKg must not be negative");
            }
            if (minWeightKg != null && (minWeightKg.signum() < 0 || minWeightKg.compareTo(maxWeightKg) > 0)) {
                throw new IllegalArgumentException("minWeightKg must be between zero and maxWeightKg");
            }
        }
    }

    public record SizeCondition(
            Set<PetSize> sizes,
            SizeConditionType type,
            PetRuleSource source
    ) {

        public SizeCondition {
            sizes = sizes == null || sizes.isEmpty()
                    ? Set.of()
                    : Set.copyOf(sizes);
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(source, "source must not be null");

            if (sizes.isEmpty()) {
                throw new IllegalArgumentException("sizes must not be empty");
            }
        }
    }

    public record SpaceRule(
            SpaceCondition condition,
            PetRuleSource source
    ) {

        public SpaceRule {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(source, "source must not be null");
        }
    }

    public record Requirement(
            RequirementType type,
            SpaceScope scope,
            PetSubject target,
            PetRuleSource source
    ) {

        public Requirement {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(scope, "scope must not be null");
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(source, "source must not be null");
        }
    }

    public record PetSubject(
            Set<PetSize> sizes,
            BreedGroup breedGroup
    ) {

        public PetSubject {
            sizes = sizes == null || sizes.isEmpty()
                    ? Set.of()
                    : Set.copyOf(sizes);
            Objects.requireNonNull(breedGroup, "breedGroup must not be null");
        }

        public static PetSubject unspecified() {
            return new PetSubject(Set.of(), BreedGroup.UNSPECIFIED);
        }

        public static PetSubject forSizes(Set<PetSize> sizes) {
            return new PetSubject(sizes, BreedGroup.UNSPECIFIED);
        }

        public static PetSubject dangerousBreeds() {
            return new PetSubject(Set.of(), BreedGroup.DANGEROUS_BREEDS);
        }
    }

    public record BreedRule(
            BreedRuleType type,
            PetRuleSource source
    ) {

        public BreedRule {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(source, "source must not be null");
        }
    }

    public record ConfirmationCondition(
            ConfirmationType type,
            PetRuleSource source
    ) {

        public ConfirmationCondition {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(source, "source must not be null");
        }
    }

    public record FeeRule(
            BigDecimal amountWon,
            PetRuleSource source
    ) {

        public FeeRule {
            Objects.requireNonNull(source, "source must not be null");
            if (amountWon != null && amountWon.signum() <= 0) {
                throw new IllegalArgumentException("amountWon must be positive");
            }
        }
    }

    public record UnparsedFragment(
            PetRuleSourceField sourceField,
            String rawText,
            UnparsedReason reason
    ) {

        public UnparsedFragment {
            Objects.requireNonNull(sourceField, "sourceField must not be null");
            Objects.requireNonNull(rawText, "rawText must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    public enum PetRuleSourceField {
        UNSPECIFIED,
        ACMPY_TYPE_CD,
        ACMPY_PSBL_CPAM,
        ACMPY_NEED_MTR,
        ETC_ACMPY_INFO,
        RELA_ACDNT_RISK_MTR,
        RELA_POSES_FCLTY,
        RELA_FRNSH_PRDLST,
        RELA_PURC_PRDLST,
        RELA_RNTL_PRDLST
    }

    public enum WeightOperator {
        LESS_THAN_OR_EQUAL,
        LESS_THAN
    }

    public enum PetSize {
        SMALL,
        MEDIUM,
        LARGE
    }

    public enum SizeConditionType {
        ALLOWED,
        DISALLOWED
    }

    public enum SpaceCondition {
        ALL_AREAS_ALLOWED,
        INDOOR_ALLOWED,
        INDOOR_PROHIBITED,
        OUTDOOR_ALLOWED,
        OUTDOOR_PROHIBITED,
        OUTDOOR_ONLY,
        TERRACE_ONLY,
        PARTIAL_AREA_ONLY
    }

    public enum RequirementType {
        LEASH,
        MUZZLE,
        WASTE_BAG,
        WASTE_DISPOSAL,
        CARRIER,
        STROLLER,
        MANNER_BELT,
        VACCINATION_STATUS,
        VACCINATION_CERTIFICATE
    }

    public enum SpaceScope {
        UNSPECIFIED,
        INDOOR,
        OUTDOOR
    }

    public enum BreedGroup {
        UNSPECIFIED,
        DANGEROUS_BREEDS
    }

    public enum BreedRuleType {
        ALL_BREEDS_ALLOWED
    }

    public enum ConfirmationType {
        RESERVATION_REQUIRED,
        INQUIRY_REQUIRED
    }

    public enum UnparsedReason {
        UNSUPPORTED_CONTENT,
        PARSER_ERROR
    }
}
