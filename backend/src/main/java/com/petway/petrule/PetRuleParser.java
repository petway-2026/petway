package com.petway.petrule;

import com.petway.petrule.PetRule.BreedGroup;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public final class PetRuleParser {

    private static final String SIZE_EXPRESSION =
            "(중소형견|소형견|중형견|대형견)";

    private static final Pattern WEIGHT_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:kg|㎏)\\s*(이하|미만)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WEIGHT_RANGE_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:kg|㎏)?\\s*[~～-]\\s*(\\d+(?:\\.\\d+)?)\\s*(?:kg|㎏)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROHIBITED_SIZE_PATTERN = Pattern.compile(
            SIZE_EXPRESSION + "\\s*(?:은|는|이|가)?\\s*"
                    + "(?:동반|출입|이용)?\\s*(?:불가|금지)"
    );

    private static final Pattern ALLOWED_SIZE_PATTERN = Pattern.compile(
            SIZE_EXPRESSION + "\\s*(?:은|는|이|가)?\\s*"
                    + "(?:(?:만\\s*)?(?:(?:동반|출입|이용)\\s*)?(?:가능|허용)|만)"
    );

    private static final Pattern BARE_SIZE_PATTERN = Pattern.compile(
            "^\\s*" + SIZE_EXPRESSION + "\\s*$"
    );

    private static final Pattern ALL_BREEDS_ALLOWED_PATTERN = Pattern.compile(
            "전\\s*견종\\s*(?:은|는|이|가)?\\s*"
                    + "(?:동반|출입|이용)?\\s*(?:가능|허용)"
    );

    private static final Pattern ALL_AREAS_PATTERN = Pattern.compile(
            "전\\s*구역\\s*(?:에서\\s*)?(?:동반|출입|이용)?\\s*(?:가능|허용)"
    );

    private static final Pattern PARTIAL_AREA_PATTERN = Pattern.compile(
            "일부\\s*구역\\s*(?:에서\\s*)?(?:만\\s*)?"
                    + "(?:동반|출입|이용)?\\s*(?:가능|허용)"
    );

    private static final Pattern TERRACE_ONLY_PATTERN = Pattern.compile(
            "(?:야외\\s*)?테라스\\s*(?:에서\\s*)?만\\s*"
                    + "(?:동반|출입|이용)?\\s*(?:가능|허용)?"
    );

    private static final Pattern OUTDOOR_ONLY_PATTERN = Pattern.compile(
            "(?:야외|외부)(?:\\s*공간)?\\s*(?:에서\\s*)?만\\s*"
                    + "(?:동반|출입|이용)?\\s*(?:가능|허용)?"
    );

    private static final Pattern INDOOR_PATTERN = Pattern.compile(
            "실내\\s*(?:동반|출입|이용)?\\s*(가능|허용|불가|금지)"
    );

    private static final Pattern OUTDOOR_PATTERN = Pattern.compile(
            "(?:야외|외부)\\s*(?:동반|출입|이용)?\\s*(가능|허용|불가|금지)"
    );

    private static final Pattern UNSAFE_POSITIVE_SPACE_PATTERN = Pattern.compile(
            "(?:불가|금지|안\\s*됨|불가능|여부|문의|확인\\s*필요)"
    );

    private static final Pattern NEGATED_REQUIREMENT_PATTERN = Pattern.compile(
            "(?:불필요|필요\\s*없음|필수\\s*아님|안\\s*함|하지\\s*않음|"
                    + "미착용(?:\\s*가능)?|없음)"
    );

    private static final Pattern LEASH_NEED_PATTERN = Pattern.compile(
            "(?:목줄|리드줄)\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)?"
    );

    private static final Pattern LEASH_EXPLICIT_PATTERN = Pattern.compile(
            "(?:목줄|리드줄)\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)"
    );

    private static final Pattern MUZZLE_NEED_PATTERN = Pattern.compile(
            "입마개\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)?"
    );

    private static final Pattern MUZZLE_EXPLICIT_PATTERN = Pattern.compile(
            "입마개\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)"
    );

    private static final Pattern WASTE_BAG_NEED_PATTERN = Pattern.compile(
            "배변\\s*봉투\\s*(?:지참|준비|휴대)?\\s*(?:필수|필요)?"
    );

    private static final Pattern WASTE_BAG_EXPLICIT_PATTERN = Pattern.compile(
            "배변\\s*봉투\\s*(?:(?:지참|준비|휴대)(?:\\s*(?:필수|필요))?|필수|필요)"
    );

    private static final Pattern WASTE_DISPOSAL_NEED_PATTERN = Pattern.compile(
            "배변\\s*처리\\s*(?:필수|필요)?"
    );

    private static final Pattern WASTE_DISPOSAL_EXPLICIT_PATTERN = Pattern.compile(
            "배변\\s*처리\\s*(?:필수|필요)"
    );

    private static final String CARRIER_EXPRESSION =
            "(?:이동장|캐리어|켄넬|케이지)"
                    + "(?:\\s*\\(\\s*(?:이동장|캐리어|켄넬|케이지)\\s*\\))?";

    private static final Pattern CARRIER_NEED_PATTERN = Pattern.compile(
            CARRIER_EXPRESSION
                    + "\\s*(?:사용|이용|착용|지참)?\\s*(?:필수|필요)?"
    );

    private static final Pattern CARRIER_EXPLICIT_PATTERN = Pattern.compile(
            CARRIER_EXPRESSION + "\\s*(?:사용|이용|착용|지참)?\\s*(?:필수|필요)"
    );

    private static final Pattern STROLLER_NEED_PATTERN = Pattern.compile(
            "유모차\\s*(?:사용|이용|지참)?\\s*(?:필수|필요)?"
    );

    private static final Pattern STROLLER_EXPLICIT_PATTERN = Pattern.compile(
            "유모차\\s*(?:사용|이용|지참)?\\s*(?:필수|필요)"
    );

    private static final Pattern MANNER_BELT_NEED_PATTERN = Pattern.compile(
            "매너\\s*벨트\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)?"
    );

    private static final Pattern MANNER_BELT_EXPLICIT_PATTERN = Pattern.compile(
            "매너\\s*벨트\\s*(?:착용|사용|지참)?\\s*(?:필수|필요)"
    );

    private static final Pattern REQUIREMENT_TARGET_PATTERN = Pattern.compile(
            "(중소형견|소형견|중형견|대형견|맹견)"
                    + "(?:의\\s*경우|은|는|이|가)?\\s*$"
    );

    private static final Pattern REQUIREMENT_SCOPE_PATTERN = Pattern.compile(
            "(실내|야외|외부|건물\\s*내|건물\\s*외)"
                    + "(?:\\s*에서)?(?:\\s+(?:시설|공간))?"
                    + "(?:\\s+(?:이동|이용))?\\s*(?:시)?"
    );

    private static final Pattern GENERIC_ALLOWANCE_PATTERN = Pattern.compile(
            "(?:반려동물|반려견)?\\s*(?:은|는|이|가)?\\s*"
                    + "동반\\s*(?:가능|허용)"
    );

    public PetRule parse(String rawText) {
        return parse(new PetRuleSource(
                PetRuleSourceField.UNSPECIFIED,
                rawText
        ));
    }

    public PetRule parse(
            PetRuleSourceField sourceField,
            String rawText
    ) {
        return parse(new PetRuleSource(sourceField, rawText));
    }

    public PetRule parse(PetRuleSource source) {
        return parse(source == null ? List.of() : List.of(source));
    }

    public PetRule parse(List<PetRuleSource> inputSources) {
        if (inputSources == null || inputSources.isEmpty()) {
            return PetRule.empty();
        }

        ParseResult result = new ParseResult();
        for (PetRuleSource source : inputSources) {
            if (source == null
                    || source.rawText() == null
                    || source.rawText().isBlank()) {
                continue;
            }

            result.sources.add(source);
            try {
                parseSource(source, result);
            } catch (RuntimeException ignored) {
                result.unparsedFragments.add(new UnparsedFragment(
                        source.sourceField(),
                        source.rawText(),
                        UnparsedReason.PARSER_ERROR
                ));
            }
        }

        return result.toPetRule();
    }

    private void parseSource(
            PetRuleSource source,
            ParseResult result
    ) {
        String rawText = source.rawText();
        boolean[] recognized = new boolean[rawText.length()];
        int parsedCountBefore = result.parsedConditionCount();

        parseWeights(source, recognized, result.weightLimits);
        parseSizes(source, recognized, result.sizeConditions);
        parseBreeds(source, recognized, result.breedRules);
        parseSpaces(source, recognized, result.spaceRules);
        parseRequirements(source, recognized, result.requirements);

        if (result.parsedConditionCount() > parsedCountBefore) {
            markAllMatches(GENERIC_ALLOWANCE_PATTERN, rawText, recognized);
        }

        result.unparsedFragments.addAll(
                extractUnparsedFragments(source, recognized)
        );
    }

    private void parseWeights(
            PetRuleSource source,
            boolean[] recognized,
            List<WeightLimit> weightLimits
    ) {
        String rawText = source.rawText();
        Matcher rangeMatcher = WEIGHT_RANGE_PATTERN.matcher(rawText);
        while (rangeMatcher.find()) {
            WeightContext context = resolveWeightContext(rawText, rangeMatcher.start());
            if (context == null) continue;
            weightLimits.add(new WeightLimit(
                    new BigDecimal(rangeMatcher.group(2)), WeightOperator.LESS_THAN_OR_EQUAL,
                    context.scope(), source, new BigDecimal(rangeMatcher.group(1))
            ));
            mark(recognized, context.recognizedStart(), rangeMatcher.end());
        }
        Matcher matcher = WEIGHT_PATTERN.matcher(rawText);
        while (matcher.find()) {
            WeightContext context = resolveWeightContext(rawText, matcher.start());
            if (context == null) {
                continue;
            }

            WeightOperator operator = "이하".equals(matcher.group(2))
                    ? WeightOperator.LESS_THAN_OR_EQUAL
                    : WeightOperator.LESS_THAN;

            weightLimits.add(new WeightLimit(
                    new BigDecimal(matcher.group(1)),
                    operator,
                    context.scope(),
                    source
            ));
            mark(recognized, context.recognizedStart(), matcher.end());
        }
    }

    private void parseSizes(
            PetRuleSource source,
            boolean[] recognized,
            List<SizeCondition> sizeConditions
    ) {
        String rawText = source.rawText();

        Matcher prohibitedMatcher = PROHIBITED_SIZE_PATTERN.matcher(rawText);
        while (prohibitedMatcher.find()) {
            sizeConditions.add(new SizeCondition(
                    toPetSizes(prohibitedMatcher.group(1)),
                    SizeConditionType.DISALLOWED,
                    source
            ));
            mark(recognized, prohibitedMatcher.start(), prohibitedMatcher.end());
        }

        Matcher allowedMatcher = ALLOWED_SIZE_PATTERN.matcher(rawText);
        while (allowedMatcher.find()) {
            if (overlaps(recognized, allowedMatcher.start(), allowedMatcher.end())) {
                continue;
            }

            sizeConditions.add(new SizeCondition(
                    toPetSizes(allowedMatcher.group(1)),
                    SizeConditionType.ALLOWED,
                    source
            ));
            mark(recognized, allowedMatcher.start(), allowedMatcher.end());
        }

        Matcher bareMatcher = BARE_SIZE_PATTERN.matcher(rawText);
        if (bareMatcher.matches()
                && !overlaps(recognized, bareMatcher.start(), bareMatcher.end())) {
            sizeConditions.add(new SizeCondition(
                    toPetSizes(bareMatcher.group(1)),
                    SizeConditionType.ALLOWED,
                    source
            ));
            mark(recognized, bareMatcher.start(), bareMatcher.end());
        }
    }

    private void parseBreeds(
            PetRuleSource source,
            boolean[] recognized,
            List<BreedRule> breedRules
    ) {
        Matcher matcher = ALL_BREEDS_ALLOWED_PATTERN.matcher(source.rawText());
        while (matcher.find()) {
            breedRules.add(new BreedRule(
                    BreedRuleType.ALL_BREEDS_ALLOWED,
                    source
            ));
            mark(recognized, matcher.start(), matcher.end());
        }
    }

    private void parseSpaces(
            PetRuleSource source,
            boolean[] recognized,
            List<SpaceRule> spaceRules
    ) {
        String rawText = source.rawText();
        if (source.sourceField() == PetRuleSourceField.ACMPY_TYPE_CD) {
            String exactValue = rawText.replaceAll("\\s+", "");
            if ("전구역동반가능".equals(exactValue)) {
                addSpaceRule(SpaceCondition.ALL_AREAS_ALLOWED, source, spaceRules);
                mark(recognized, 0, rawText.length());
                return;
            }
            if ("일부구역동반가능".equals(exactValue)) {
                addSpaceRule(SpaceCondition.PARTIAL_AREA_ONLY, source, spaceRules);
                mark(recognized, 0, rawText.length());
                return;
            }
        }

        parseSpacePattern(
                ALL_AREAS_PATTERN,
                SpaceCondition.ALL_AREAS_ALLOWED,
                source,
                recognized,
                spaceRules
        );
        parseSpacePattern(
                PARTIAL_AREA_PATTERN,
                SpaceCondition.PARTIAL_AREA_ONLY,
                source,
                recognized,
                spaceRules
        );
        parseSpacePattern(
                TERRACE_ONLY_PATTERN,
                SpaceCondition.TERRACE_ONLY,
                source,
                recognized,
                spaceRules
        );
        parseSpacePattern(
                OUTDOOR_ONLY_PATTERN,
                SpaceCondition.OUTDOOR_ONLY,
                source,
                recognized,
                spaceRules
        );
        parseAllowedOrProhibitedSpace(
                INDOOR_PATTERN,
                SpaceCondition.INDOOR_ALLOWED,
                SpaceCondition.INDOOR_PROHIBITED,
                source,
                recognized,
                spaceRules
        );
        parseAllowedOrProhibitedSpace(
                OUTDOOR_PATTERN,
                SpaceCondition.OUTDOOR_ALLOWED,
                SpaceCondition.OUTDOOR_PROHIBITED,
                source,
                recognized,
                spaceRules
        );
    }

    private void parseSpacePattern(
            Pattern pattern,
            SpaceCondition condition,
            PetRuleSource source,
            boolean[] recognized,
            List<SpaceRule> spaceRules
    ) {
        Matcher matcher = pattern.matcher(source.rawText());
        while (matcher.find()) {
            if (overlaps(recognized, matcher.start(), matcher.end())) {
                continue;
            }
            if (hasClauseMatch(
                    UNSAFE_POSITIVE_SPACE_PATTERN,
                    source.rawText(),
                    matcher.start(),
                    matcher.end()
            )) {
                continue;
            }
            addSpaceRule(condition, source, spaceRules);
            mark(recognized, matcher.start(), matcher.end());
        }
    }

    private void parseAllowedOrProhibitedSpace(
            Pattern pattern,
            SpaceCondition allowed,
            SpaceCondition prohibited,
            PetRuleSource source,
            boolean[] recognized,
            List<SpaceRule> spaceRules
    ) {
        Matcher matcher = pattern.matcher(source.rawText());
        while (matcher.find()) {
            if (overlaps(recognized, matcher.start(), matcher.end())) {
                continue;
            }

            String expression = matcher.group(1);
            boolean isAllowed = "가능".equals(expression) || "허용".equals(expression);
            if (isAllowed && hasClauseMatch(
                    UNSAFE_POSITIVE_SPACE_PATTERN,
                    source.rawText(),
                    matcher.start(),
                    matcher.end()
            )) {
                continue;
            }
            addSpaceRule(
                    isAllowed ? allowed : prohibited,
                    source,
                    spaceRules
            );
            mark(recognized, matcher.start(), matcher.end());
        }
    }

    private void addSpaceRule(
            SpaceCondition condition,
            PetRuleSource source,
            List<SpaceRule> spaceRules
    ) {
        spaceRules.add(new SpaceRule(condition, source));
    }

    private void parseRequirements(
            PetRuleSource source,
            boolean[] recognized,
            List<Requirement> requirements
    ) {
        boolean needMatterSource =
                source.sourceField() == PetRuleSourceField.ACMPY_NEED_MTR;

        parseRequirement(
                needMatterSource ? LEASH_NEED_PATTERN : LEASH_EXPLICIT_PATTERN,
                RequirementType.LEASH,
                source,
                recognized,
                requirements
        );
        parseRequirement(
                needMatterSource ? MUZZLE_NEED_PATTERN : MUZZLE_EXPLICIT_PATTERN,
                RequirementType.MUZZLE,
                source,
                recognized,
                requirements
        );
        parseRequirement(
                needMatterSource ? WASTE_BAG_NEED_PATTERN : WASTE_BAG_EXPLICIT_PATTERN,
                RequirementType.WASTE_BAG,
                source,
                recognized,
                requirements
        );
        parseRequirement(
                needMatterSource
                        ? WASTE_DISPOSAL_NEED_PATTERN
                        : WASTE_DISPOSAL_EXPLICIT_PATTERN,
                RequirementType.WASTE_DISPOSAL,
                source,
                recognized,
                requirements
        );
        parseRequirement(
                needMatterSource ? CARRIER_NEED_PATTERN : CARRIER_EXPLICIT_PATTERN,
                RequirementType.CARRIER,
                source,
                recognized,
                requirements
        );
        parseRequirement(
                needMatterSource ? STROLLER_NEED_PATTERN : STROLLER_EXPLICIT_PATTERN,
                RequirementType.STROLLER,
                source,
                recognized,
                requirements
        );
        parseRequirement(
                needMatterSource
                        ? MANNER_BELT_NEED_PATTERN
                        : MANNER_BELT_EXPLICIT_PATTERN,
                RequirementType.MANNER_BELT,
                source,
                recognized,
                requirements
        );
    }

    private void parseRequirement(
            Pattern pattern,
            RequirementType type,
            PetRuleSource source,
            boolean[] recognized,
            List<Requirement> requirements
    ) {
        Matcher matcher = pattern.matcher(source.rawText());
        while (matcher.find()) {
            if (overlaps(recognized, matcher.start(), matcher.end())) {
                continue;
            }
            if (hasClauseMatch(
                    NEGATED_REQUIREMENT_PATTERN,
                    source.rawText(),
                    matcher.start(),
                    matcher.end()
            )) {
                continue;
            }

            PrefixContext prefixContext = findPrefixContext(
                    source.rawText(),
                    matcher.start()
            );
            requirements.add(new Requirement(
                    type,
                    prefixContext.scope(),
                    prefixContext.target(),
                    source
            ));
            mark(
                    recognized,
                    Math.min(matcher.start(), prefixContext.recognizedStart()),
                    matcher.end()
            );
        }
    }

    private PrefixContext findPrefixContext(
            String rawText,
            int requirementStart
    ) {
        int clauseStart = findClauseStart(rawText, requirementStart);
        String prefix = rawText.substring(clauseStart, requirementStart);
        int recognizedStart = requirementStart;
        SpaceScope scope = SpaceScope.UNSPECIFIED;
        PetSubject target = PetSubject.unspecified();

        Matcher scopeMatcher = REQUIREMENT_SCOPE_PATTERN.matcher(prefix);
        while (scopeMatcher.find()) {
            scope = toSpaceScope(scopeMatcher.group(1));
            recognizedStart = Math.min(
                    recognizedStart,
                    clauseStart + scopeMatcher.start()
            );
        }

        Matcher targetMatcher = REQUIREMENT_TARGET_PATTERN.matcher(prefix);
        while (targetMatcher.find()) {
            target = toPetSubject(targetMatcher.group(1));
            recognizedStart = Math.min(
                    recognizedStart,
                    clauseStart + targetMatcher.start()
            );
        }

        return new PrefixContext(target, scope, recognizedStart);
    }

    private List<UnparsedFragment> extractUnparsedFragments(
            PetRuleSource source,
            boolean[] recognized
    ) {
        List<UnparsedFragment> fragments = new ArrayList<>();
        String rawText = source.rawText();
        int index = 0;

        while (index < rawText.length()) {
            while (index < rawText.length() && recognized[index]) {
                index++;
            }
            int start = index;
            while (index < rawText.length() && !recognized[index]) {
                index++;
            }

            if (start < index) {
                String fragment = cleanFragment(rawText.substring(start, index));
                if (hasMeaningfulContent(fragment)) {
                    fragments.add(new UnparsedFragment(
                            source.sourceField(),
                            fragment,
                            UnparsedReason.UNSUPPORTED_CONTENT
                    ));
                }
            }
        }

        return fragments;
    }

    private WeightContext resolveWeightContext(
            String rawText,
            int weightStart
    ) {
        int clauseStart = findClauseStart(rawText, weightStart);
        String prefix = rawText.substring(clauseStart, weightStart).trim();
        String normalized = prefix
                .replaceFirst("^(?:및|그리고|또는|과|와)\\s*", "")
                .replaceAll("\\s+", "");

        return switch (normalized) {
            case "" -> new WeightContext(
                    SpaceScope.UNSPECIFIED,
                    weightStart
            );
            case "실내", "실내에서" -> new WeightContext(
                    SpaceScope.INDOOR,
                    clauseStart
            );
            case "야외", "야외에서", "외부", "외부에서" -> new WeightContext(
                    SpaceScope.OUTDOOR,
                    clauseStart
            );
            case "전체", "전체장소", "반려견", "반려동물" -> new WeightContext(
                    SpaceScope.UNSPECIFIED,
                    clauseStart
            );
            default -> null;
        };
    }

    private boolean hasClauseMatch(
            Pattern pattern,
            String rawText,
            int matchStart,
            int matchEnd
    ) {
        int clauseStart = findClauseStart(rawText, matchStart);
        int clauseEnd = findClauseEnd(rawText, matchEnd);
        return pattern.matcher(rawText.substring(clauseStart, clauseEnd)).find();
    }

    private int findClauseStart(String rawText, int index) {
        for (int current = index - 1; current >= 0; current--) {
            if (isClauseBoundary(rawText.charAt(current))) {
                return current + 1;
            }
        }
        return 0;
    }

    private int findClauseEnd(String rawText, int index) {
        for (int current = index; current < rawText.length(); current++) {
            if (isClauseBoundary(rawText.charAt(current))) {
                return current;
            }
        }
        return rawText.length();
    }

    private boolean isClauseBoundary(char value) {
        return value == ','
                || value == ';'
                || value == '.'
                || value == '\n'
                || value == '\r';
    }

    private String cleanFragment(String value) {
        return value
                .replaceAll("^[\\s,.;:!?/()\\[\\]{}·ㆍ]+", "")
                .replaceAll("[\\s,.;:!?/()\\[\\]{}·ㆍ]+$", "")
                .replaceAll("^(?:및|그리고|또는|과|와)\\s*", "")
                .replaceAll("\\s*(?:및|그리고|또는|과|와)$", "")
                .trim();
    }

    private boolean hasMeaningfulContent(String value) {
        if (value.isBlank()) {
            return false;
        }

        String normalized = value.replaceAll(
                "[\\s,.;:!?/()\\[\\]{}·ㆍ]+|(?:및|그리고|또는|과|와|이며|이고|시|은|는|이|가|을|를)",
                ""
        );
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
        for (int index = Math.max(0, start); index < Math.min(end, recognized.length); index++) {
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

    private Set<PetSize> toPetSizes(String value) {
        return switch (value) {
            case "소형견" -> Set.of(PetSize.SMALL);
            case "중형견" -> Set.of(PetSize.MEDIUM);
            case "대형견" -> Set.of(PetSize.LARGE);
            case "중소형견" -> Set.of(PetSize.SMALL, PetSize.MEDIUM);
            default -> throw new IllegalArgumentException("Unsupported pet size: " + value);
        };
    }

    private PetSubject toPetSubject(String value) {
        if ("맹견".equals(value)) {
            return PetSubject.dangerousBreeds();
        }
        return PetSubject.forSizes(toPetSizes(value));
    }

    private SpaceScope toSpaceScope(String value) {
        String normalized = value.replaceAll("\\s+", "");
        if ("실내".equals(normalized) || "건물내".equals(normalized)) {
            return SpaceScope.INDOOR;
        }
        if ("야외".equals(normalized)
                || "외부".equals(normalized)
                || "건물외".equals(normalized)) {
            return SpaceScope.OUTDOOR;
        }
        return SpaceScope.UNSPECIFIED;
    }

    private record PrefixContext(
            PetSubject target,
            SpaceScope scope,
            int recognizedStart
    ) {
    }

    private record WeightContext(
            SpaceScope scope,
            int recognizedStart
    ) {
    }

    private static final class ParseResult {

        private final List<PetRuleSource> sources = new ArrayList<>();
        private final List<WeightLimit> weightLimits = new ArrayList<>();
        private final List<SizeCondition> sizeConditions = new ArrayList<>();
        private final List<SpaceRule> spaceRules = new ArrayList<>();
        private final List<Requirement> requirements = new ArrayList<>();
        private final List<BreedRule> breedRules = new ArrayList<>();
        private final List<UnparsedFragment> unparsedFragments = new ArrayList<>();

        private int parsedConditionCount() {
            return weightLimits.size()
                    + sizeConditions.size()
                    + spaceRules.size()
                    + requirements.size()
                    + breedRules.size();
        }

        private PetRule toPetRule() {
            return new PetRule(
                    sources,
                    weightLimits,
                    sizeConditions,
                    spaceRules,
                    requirements,
                    breedRules,
                    unparsedFragments
            );
        }
    }
}
