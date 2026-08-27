package com.petway.petrule;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

public record PetRule(
        String rawText,
        WeightLimit weightLimit,
        Set<PetSize> allowedSizes,
        Set<PetSize> disallowedSizes,
        Set<SpaceCondition> spaceConditions,
        Set<Requirement> requirements,
        boolean hasUnknownContent
) {

    public PetRule {
        allowedSizes = immutableCopy(allowedSizes);
        disallowedSizes = immutableCopy(disallowedSizes);
        spaceConditions = immutableCopy(spaceConditions);
        requirements = immutableCopy(requirements);
    }

    public static PetRule unknown(String rawText) {
        return new PetRule(
                rawText,
                null,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                true
        );
    }

    public boolean hasParsedCondition() {
        return weightLimit != null
                || !allowedSizes.isEmpty()
                || !disallowedSizes.isEmpty()
                || !spaceConditions.isEmpty()
                || !requirements.isEmpty();
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        return values == null || values.isEmpty()
                ? Set.of()
                : Set.copyOf(values);
    }

    public record WeightLimit(
            BigDecimal maxWeightKg,
            WeightOperator operator
    ) {

        public WeightLimit {
            Objects.requireNonNull(maxWeightKg, "maxWeightKg must not be null");
            Objects.requireNonNull(operator, "operator must not be null");

            if (maxWeightKg.signum() < 0) {
                throw new IllegalArgumentException("maxWeightKg must not be negative");
            }
        }
    }

    public record Requirement(
            RequirementType type,
            SpaceScope scope
    ) {

        public Requirement {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(scope, "scope must not be null");
        }
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

    public enum SpaceCondition {
        INDOOR_ALLOWED,
        INDOOR_PROHIBITED,
        OUTDOOR_ALLOWED,
        OUTDOOR_PROHIBITED,
        TERRACE_ONLY,
        PARTIAL_AREA_ONLY
    }

    public enum RequirementType {
        CARRIER,
        LEASH
    }

    public enum SpaceScope {
        UNSPECIFIED,
        INDOOR,
        OUTDOOR
    }
}
