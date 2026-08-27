package com.petway.petrule;

import com.petway.petrule.PetRule.PetSize;
import com.petway.petrule.PetRule.Requirement;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SpaceCondition;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.petrule.PetRule.WeightLimit;
import com.petway.petrule.PetRule.WeightOperator;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PetRuleParser {

    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*kg\\s*(이하|미만)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROHIBITED_SIZE_PATTERN = Pattern.compile(
            "(소형견|중형견|대형견)\\s*(?:은|는|이|가)?\\s*"
                    + "(?:동반|출입|이용)?\\s*(?:불가|금지)"
    );

    private static final Pattern ALLOWED_SIZE_PATTERN = Pattern.compile(
            "(소형견|중형견|대형견)\\s*(?:은|는|이|가)?\\s*"
                    + "(?:만\\s*)?(?:(?:동반|출입|이용)\\s*)?(?:가능|허용)?"
    );

    private static final Pattern INDOOR_PATTERN = Pattern.compile(
            "실내\\s*(?:동반|출입|이용)?\\s*(가능|허용|불가|금지)"
    );

    private static final Pattern OUTDOOR_PATTERN = Pattern.compile(
            "야외\\s*(?:동반|출입|이용)?\\s*(가능|허용|불가|금지)"
    );

    private static final Pattern TERRACE_ONLY_PATTERN = Pattern.compile(
            "테라스\\s*(?:에서\\s*)?만\\s*(?:동반|출입|이용)?\\s*(?:가능|허용)?"
    );

    private static final Pattern PARTIAL_AREA_ONLY_PATTERN = Pattern.compile(
            "일부\\s*구역\\s*(?:에서\\s*)?만\\s*(?:동반|출입|이용)?\\s*(?:가능|허용)?"
    );

    private static final Pattern CARRIER_PATTERN = Pattern.compile(
            "(?:(실내|야외)\\s*)?(?:이동장|캐리어)\\s*"
                    + "(?:사용|이용|착용)?\\s*(?:필수|필요)"
    );

    private static final Pattern LEASH_PATTERN = Pattern.compile(
            "(?:(실내|야외)\\s*)?목줄\\s*(?:착용\\s*)?(?:필수|필요|시)"
    );

    private static final Pattern GENERIC_ALLOWANCE_PATTERN = Pattern.compile(
            "(?:반려동물|반려견)?\\s*(?:은|는|이|가)?\\s*동반\\s*(?:가능|허용)"
    );

    public PetRule parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return PetRule.unknown(rawText);
        }

        try {
            return parseSafely(rawText);
        } catch (RuntimeException ignored) {
            return PetRule.unknown(rawText);
        }
    }

    private PetRule parseSafely(String rawText) {
        boolean[] recognized = new boolean[rawText.length()];

        WeightLimit weightLimit = parseWeight(rawText, recognized);
        Set<PetSize> allowedSizes = EnumSet.noneOf(PetSize.class);
        Set<PetSize> disallowedSizes = EnumSet.noneOf(PetSize.class);
        Set<SpaceCondition> spaceConditions = EnumSet.noneOf(SpaceCondition.class);
        Set<Requirement> requirements = new java.util.HashSet<>();

        parseSizes(rawText, recognized, allowedSizes, disallowedSizes);
        parseSpaces(rawText, recognized, spaceConditions);
        parseRequirements(rawText, recognized, requirements);

        boolean hasParsedCondition = weightLimit != null
                || !allowedSizes.isEmpty()
                || !disallowedSizes.isEmpty()
                || !spaceConditions.isEmpty()
                || !requirements.isEmpty();

        if (hasParsedCondition) {
            markAllMatches(GENERIC_ALLOWANCE_PATTERN, rawText, recognized);
        }

        return new PetRule(
                rawText,
                weightLimit,
                allowedSizes,
                disallowedSizes,
                spaceConditions,
                requirements,
                hasUnknownContent(rawText, recognized, hasParsedCondition)
        );
    }

    private WeightLimit parseWeight(String rawText, boolean[] recognized) {
        Matcher matcher = WEIGHT_PATTERN.matcher(rawText);
        if (!matcher.find()) {
            return null;
        }

        mark(recognized, matcher.start(), matcher.end());

        WeightOperator operator = "이하".equals(matcher.group(2))
                ? WeightOperator.LESS_THAN_OR_EQUAL
                : WeightOperator.LESS_THAN;

        return new WeightLimit(
                new BigDecimal(matcher.group(1)),
                operator
        );
    }

    private void parseSizes(
            String rawText,
            boolean[] recognized,
            Set<PetSize> allowedSizes,
            Set<PetSize> disallowedSizes
    ) {
        Matcher prohibitedMatcher = PROHIBITED_SIZE_PATTERN.matcher(rawText);
        while (prohibitedMatcher.find()) {
            disallowedSizes.add(toPetSize(prohibitedMatcher.group(1)));
            mark(recognized, prohibitedMatcher.start(), prohibitedMatcher.end());
        }

        Matcher allowedMatcher = ALLOWED_SIZE_PATTERN.matcher(rawText);
        while (allowedMatcher.find()) {
            if (overlaps(recognized, allowedMatcher.start(), allowedMatcher.end())) {
                continue;
            }

            allowedSizes.add(toPetSize(allowedMatcher.group(1)));
            mark(recognized, allowedMatcher.start(), allowedMatcher.end());
        }
    }

    private void parseSpaces(
            String rawText,
            boolean[] recognized,
            Set<SpaceCondition> spaceConditions
    ) {
        parseAllowedOrProhibitedSpace(
                INDOOR_PATTERN,
                rawText,
                recognized,
                SpaceCondition.INDOOR_ALLOWED,
                SpaceCondition.INDOOR_PROHIBITED,
                spaceConditions
        );
        parseAllowedOrProhibitedSpace(
                OUTDOOR_PATTERN,
                rawText,
                recognized,
                SpaceCondition.OUTDOOR_ALLOWED,
                SpaceCondition.OUTDOOR_PROHIBITED,
                spaceConditions
        );

        if (markAllMatches(TERRACE_ONLY_PATTERN, rawText, recognized)) {
            spaceConditions.add(SpaceCondition.TERRACE_ONLY);
        }
        if (markAllMatches(PARTIAL_AREA_ONLY_PATTERN, rawText, recognized)) {
            spaceConditions.add(SpaceCondition.PARTIAL_AREA_ONLY);
        }
    }

    private void parseAllowedOrProhibitedSpace(
            Pattern pattern,
            String rawText,
            boolean[] recognized,
            SpaceCondition allowed,
            SpaceCondition prohibited,
            Set<SpaceCondition> spaceConditions
    ) {
        Matcher matcher = pattern.matcher(rawText);
        while (matcher.find()) {
            String expression = matcher.group(1);
            spaceConditions.add(
                    "가능".equals(expression) || "허용".equals(expression)
                            ? allowed
                            : prohibited
            );
            mark(recognized, matcher.start(), matcher.end());
        }
    }

    private void parseRequirements(
            String rawText,
            boolean[] recognized,
            Set<Requirement> requirements
    ) {
        Matcher carrierMatcher = CARRIER_PATTERN.matcher(rawText);
        while (carrierMatcher.find()) {
            requirements.add(new Requirement(
                    RequirementType.CARRIER,
                    toSpaceScope(carrierMatcher.group(1))
            ));
            mark(recognized, carrierMatcher.start(), carrierMatcher.end());
        }

        Matcher leashMatcher = LEASH_PATTERN.matcher(rawText);
        while (leashMatcher.find()) {
            requirements.add(new Requirement(
                    RequirementType.LEASH,
                    toSpaceScope(leashMatcher.group(1))
            ));
            mark(recognized, leashMatcher.start(), leashMatcher.end());
        }
    }

    private boolean hasUnknownContent(
            String rawText,
            boolean[] recognized,
            boolean hasParsedCondition
    ) {
        StringBuilder remaining = new StringBuilder();
        for (int index = 0; index < rawText.length(); index++) {
            if (!recognized[index]) {
                remaining.append(rawText.charAt(index));
            }
        }

        String normalized = remaining.toString()
                .replaceAll("[\\s,.;:!?/()\\[\\]{}·ㆍ]+", "");

        if (hasParsedCondition) {
            normalized = normalized.replaceAll(
                    "(?:및|그리고|또는|과|와|이며|이고|시|은|는|이|가|을|를)",
                    ""
            );
        }

        return !normalized.isBlank();
    }

    private boolean markAllMatches(
            Pattern pattern,
            String rawText,
            boolean[] recognized
    ) {
        boolean found = false;
        Matcher matcher = pattern.matcher(rawText);
        while (matcher.find()) {
            mark(recognized, matcher.start(), matcher.end());
            found = true;
        }
        return found;
    }

    private void mark(boolean[] recognized, int start, int end) {
        for (int index = start; index < end; index++) {
            recognized[index] = true;
        }
    }

    private boolean overlaps(boolean[] recognized, int start, int end) {
        for (int index = start; index < end; index++) {
            if (recognized[index]) {
                return true;
            }
        }
        return false;
    }

    private PetSize toPetSize(String value) {
        return switch (value) {
            case "소형견" -> PetSize.SMALL;
            case "중형견" -> PetSize.MEDIUM;
            case "대형견" -> PetSize.LARGE;
            default -> throw new IllegalArgumentException("Unsupported pet size: " + value);
        };
    }

    private SpaceScope toSpaceScope(String value) {
        if ("실내".equals(value)) {
            return SpaceScope.INDOOR;
        }
        if ("야외".equals(value)) {
            return SpaceScope.OUTDOOR;
        }
        return SpaceScope.UNSPECIFIED;
    }
}
