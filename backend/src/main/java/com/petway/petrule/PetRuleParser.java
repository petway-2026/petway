/* 원문 필드의 문맥을 유지하면서 정규식으로 동반 규정을 추출한다. 최종 동반 가능 판정은 하지 않는다. */
package com.petway.petrule;

import com.petway.petrule.PetRule.BreedGroup;
import com.petway.petrule.PetRule.BreedRule;
import com.petway.petrule.PetRule.BreedRuleType;
import com.petway.petrule.PetRule.ConfirmationCondition;
import com.petway.petrule.PetRule.ConfirmationType;
import com.petway.petrule.PetRule.FeeRule;
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

import static com.petway.petrule.PetRulePatterns.*;

@Component
public final class PetRuleParser {

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

    // 빈 원문은 건너뛰고 필드마다 독립적으로 파싱한다. 한 필드의 오류가 다른 규정 추출을 중단하지 않는다.
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
                ParseResult sourceResult = new ParseResult();
                parseSource(source, sourceResult);
                result.merge(sourceResult);
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
        // 이미 해석한 문자 범위를 기록한다. 중복 해석을 피하고 남은 의미 있는 문장을 미해석 근거로 보존한다.
        boolean[] recognized = new boolean[rawText.length()];
        int parsedCountBefore = result.parsedConditionCount();

        parseWeights(source, recognized, result.weightLimits);
        parseSizes(source, recognized, result.sizeConditions);
        parseBreeds(source, recognized, result.breedRules);
        parseSpaces(source, recognized, result.spaceRules);
        parseRequirements(source, recognized, result.requirements);
        parseConfirmationConditions(source, recognized, result.confirmationConditions);
        parseFees(source, recognized, result.feeRules);

        if (result.parsedConditionCount() > parsedCountBefore) {
            markAllMatches(GENERIC_ALLOWANCE_PATTERN, rawText, recognized);
        }

        result.unparsedFragments.addAll(
                extractUnparsedFragments(source, recognized)
        );
    }

    // 체중 범위와 상한을 숫자·연산자·공간으로 분리한다. 원문은 변형하지 않고 조건의 출처로 유지한다.
    private void parseWeights(
            PetRuleSource source,
            boolean[] recognized,
            List<WeightLimit> weightLimits
    ) {
        String rawText = source.rawText();
        // 방문 날짜를 받지 않는 판정에서 요일별 예외를 장소 전체의 체중 상한으로 확정하지 않는다.
        if (DAY_DEPENDENT_RULE_PATTERN.matcher(rawText).find()) return;
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

    // 크기 목록·금지·허용 표현을 나누어 추출하고 이미 인식한 범위는 다시 허용 조건으로 읽지 않는다.
    private void parseSizes(
            PetRuleSource source,
            boolean[] recognized,
            List<SizeCondition> sizeConditions
    ) {
        String rawText = source.rawText();

        Matcher mediumSmallMatcher = MEDIUM_SMALL_LIST_PATTERN.matcher(rawText);
        while (mediumSmallMatcher.find()) {
            if (overlaps(recognized, mediumSmallMatcher.start(), mediumSmallMatcher.end())) {
                continue;
            }
            sizeConditions.add(new SizeCondition(
                    Set.of(PetSize.SMALL, PetSize.MEDIUM),
                    SizeConditionType.ALLOWED,
                    source
            ));
            mark(recognized, mediumSmallMatcher.start(), mediumSmallMatcher.end());
        }

        Matcher prohibitedMatcher = PROHIBITED_SIZE_PATTERN.matcher(rawText);
        while (prohibitedMatcher.find()) {
            if (!isUnconditionalProhibition(rawText, prohibitedMatcher.start(), prohibitedMatcher.end())) continue;
            sizeConditions.add(new SizeCondition(
                    toPetSizes(prohibitedMatcher.group(1)),
                    SizeConditionType.DISALLOWED,
                    source
            ));
            mark(recognized, prohibitedMatcher.start(), prohibitedMatcher.end());
        }

        Matcher allowedMatcher = ALLOWED_SIZE_PATTERN.matcher(rawText);
        while (allowedMatcher.find()) {
            if (hasClauseMatch(UNSAFE_POSITIVE_SPACE_PATTERN, rawText, allowedMatcher.start(), allowedMatcher.end())) {
                continue;
            }
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

        // 문화정보원 허용 크기에는 '5kg 미만 소형', '소형/대형' 같은 축약형도 들어온다.
        if (source.sourceField() == PetRuleSourceField.ACMPY_PSBL_CPAM) {
            Matcher labelMatcher = CAPACITY_SIZE_LABEL_PATTERN.matcher(rawText);
            while (labelMatcher.find()) {
                if (overlaps(recognized, labelMatcher.start(), labelMatcher.end())) continue;
                sizeConditions.add(new SizeCondition(
                        toPetSizes(labelMatcher.group(1) + "견"), SizeConditionType.ALLOWED, source));
                mark(recognized, labelMatcher.start(), labelMatcher.end());
            }
        }
    }

    private void parseBreeds(
            PetRuleSource source,
            boolean[] recognized,
            List<BreedRule> breedRules
    ) {
        Matcher matcher = ALL_BREEDS_ALLOWED_PATTERN.matcher(source.rawText());
        while (matcher.find()) {
            if (hasClauseMatch(UNSAFE_POSITIVE_SPACE_PATTERN, source.rawText(), matcher.start(), matcher.end())) continue;
            breedRules.add(new BreedRule(
                    BreedRuleType.ALL_BREEDS_ALLOWED,
                    source
            ));
            mark(recognized, matcher.start(), matcher.end());
        }
        Matcher prohibitedMatcher = DANGEROUS_BREEDS_PROHIBITED_PATTERN.matcher(source.rawText());
        while (prohibitedMatcher.find()) {
            if (!isUnconditionalProhibition(source.rawText(), prohibitedMatcher.start(), prohibitedMatcher.end())) continue;
            breedRules.add(new BreedRule(BreedRuleType.DANGEROUS_BREEDS_PROHIBITED, source));
            mark(recognized, prohibitedMatcher.start(), prohibitedMatcher.end());
        }
    }

    private boolean isUnconditionalProhibition(String rawText, int start, int end) {
        String prefix = rawText.substring(findClauseStart(rawText, start), start);
        String suffix = rawText.substring(end, findClauseEnd(rawText, end));
        return !CONDITIONAL_PROHIBITION_PREFIX.matcher(prefix).find()
                && !NEGATED_PROHIBITION_SUFFIX.matcher(suffix).find();
    }

    private void parseSpaces(
            PetRuleSource source,
            boolean[] recognized,
            List<SpaceRule> spaceRules
    ) {
        String rawText = source.rawText();
        // 동반 유형 필드의 정형 값은 정확한 일치를 우선하고, 그 외 문장에는 공간 패턴을 적용한다.
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
            if (!isAllowed && !isUnconditionalProhibition(source.rawText(), matcher.start(), matcher.end())) {
                continue;
            }
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
        // '동반 시 필요사항' 필드는 자체적으로 의무 문맥이다. 다른 필드에서는 명시적 필수 표현을 더 요구한다.
        boolean needMatterSource =
                source.sourceField() == PetRuleSourceField.ACMPY_NEED_MTR;

        parseVaccinationCertificate(
                needMatterSource
                        ? VACCINATION_CERTIFICATE_NEED_PATTERN
                        : VACCINATION_CERTIFICATE_EXPLICIT_PATTERN,
                source,
                recognized,
                requirements
        );
        for (RequirementPattern rule : REQUIREMENT_PATTERNS) {
            parseRequirement(rule.forSource(needMatterSource), rule.type(), source, recognized, requirements);
        }
    }

    private void parseVaccinationCertificate(
            Pattern pattern,
            PetRuleSource source,
            boolean[] recognized,
            List<Requirement> requirements
    ) {
        Matcher matcher = pattern.matcher(source.rawText());
        while (matcher.find()) {
            if (overlaps(recognized, matcher.start(), matcher.end())
                    || hasClauseMatch(
                    NEGATED_REQUIREMENT_PATTERN,
                    source.rawText(),
                    matcher.start(),
                    matcher.end()
            )) {
                continue;
            }
            requirements.add(new Requirement(
                    RequirementType.VACCINATION_STATUS,
                    SpaceScope.UNSPECIFIED,
                    PetSubject.unspecified(),
                    source
            ));
            requirements.add(new Requirement(
                    RequirementType.VACCINATION_CERTIFICATE,
                    SpaceScope.UNSPECIFIED,
                    PetSubject.unspecified(),
                    source
            ));
            mark(recognized, matcher.start(), matcher.end());
        }
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
        // A comma after a subject header does not end its condition:
        // "맹견의 경우, 입마개 착용 필수". Other commas remain clause boundaries.
        if (clauseStart > 0 && rawText.charAt(clauseStart - 1) == ',') {
            int previousStart = findClauseStart(rawText, clauseStart - 1);
            String header = rawText.substring(previousStart, clauseStart - 1);
            if (REQUIREMENT_TARGET_PATTERN.matcher(header).find()) {
                clauseStart = previousStart;
            }
        }
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

        // Space qualifiers can follow the subject without changing the target.
        String targetPrefix = REQUIREMENT_SCOPE_PATTERN.matcher(prefix).replaceAll("")
                .replace(',', ' ');
        Matcher targetMatcher = REQUIREMENT_TARGET_PATTERN.matcher(targetPrefix);
        while (targetMatcher.find()) {
            target = toPetSubject(targetMatcher.group(1));
            recognizedStart = Math.min(
                    recognizedStart,
                    clauseStart + prefix.lastIndexOf(targetMatcher.group(1))
            );
        }

        Matcher sexMatcher = SEX_QUALIFIER_PATTERN.matcher(prefix);
        while (sexMatcher.find()) {
            recognizedStart = Math.min(
                    recognizedStart,
                    clauseStart + sexMatcher.start()
            );
        }

        return new PrefixContext(target, scope, recognizedStart);
    }

    private void parseConfirmationConditions(
            PetRuleSource source,
            boolean[] recognized,
            List<ConfirmationCondition> confirmationConditions
    ) {
        parseConfirmationCondition(
                RESERVATION_REQUIRED_PATTERN,
                ConfirmationType.RESERVATION_REQUIRED,
                source,
                recognized,
                confirmationConditions
        );
        parseConfirmationCondition(
                INQUIRY_REQUIRED_PATTERN,
                ConfirmationType.INQUIRY_REQUIRED,
                source,
                recognized,
                confirmationConditions
        );
    }

    private void parseConfirmationCondition(
            Pattern pattern,
            ConfirmationType type,
            PetRuleSource source,
            boolean[] recognized,
            List<ConfirmationCondition> confirmationConditions
    ) {
        Matcher matcher = pattern.matcher(source.rawText());
        while (matcher.find()) {
            if (overlaps(recognized, matcher.start(), matcher.end())) {
                continue;
            }
            confirmationConditions.add(new ConfirmationCondition(type, source));
            // The lead-in is part of the same confirmation clause. For example,
            // "거리 내 매장은 개별 정책이 상이하므로 개별 문의 필요" must be
            // consumed as one clause; otherwise the unparsed lead-in is later
            // reported as an additional generic confirmation reason.
            mark(recognized, findClauseStart(source.rawText(), matcher.start()), matcher.end());
        }
    }

    private void parseFees(
            PetRuleSource source,
            boolean[] recognized,
            List<FeeRule> feeRules
    ) {
        Matcher amountMatcher = PET_FEE_AMOUNT_PATTERN.matcher(source.rawText());
        while (amountMatcher.find()) {
            if (overlaps(recognized, amountMatcher.start(), amountMatcher.end())
                    || hasClauseMatch(
                    NEGATED_REQUIREMENT_PATTERN,
                    source.rawText(),
                    amountMatcher.start(),
                    amountMatcher.end()
            )) {
                continue;
            }
            BigDecimal amountWon = new BigDecimal(amountMatcher.group(1).replace(",", ""));
            if (amountWon.signum() <= 0) {
                continue;
            }
            feeRules.add(new FeeRule(amountWon, source));
            mark(recognized, amountMatcher.start(), amountMatcher.end());
        }

        Matcher requiredMatcher = PET_FEE_REQUIRED_PATTERN.matcher(source.rawText());
        while (requiredMatcher.find()) {
            if (overlaps(recognized, requiredMatcher.start(), requiredMatcher.end())
                    || hasClauseMatch(
                    NEGATED_REQUIREMENT_PATTERN,
                    source.rawText(),
                    requiredMatcher.start(),
                    requiredMatcher.end()
            )
                    || hasClauseMatch(
                    UNCERTAIN_FEE_PATTERN,
                    source.rawText(),
                    requiredMatcher.start(),
                    requiredMatcher.end()
            )) {
                continue;
            }
            feeRules.add(new FeeRule(null, source));
            mark(recognized, requiredMatcher.start(), requiredMatcher.end());
        }
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
        String normalized = cleanFragment(prefix)
                .replaceFirst("^(?:및|그리고|또는|과|와)\\s*", "")
                .replaceAll("[()\\[\\]]", "")
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
            case "야외", "야외에서", "실외", "실외에서", "외부", "외부에서" -> new WeightContext(
                    SpaceScope.OUTDOOR,
                    clauseStart
            );
            case "전체", "전체장소", "반려견", "반려동물" -> new WeightContext(
                    SpaceScope.UNSPECIFIED,
                    clauseStart
            );
            default -> normalized.matches(".*(?:소형견|중형견|중소형견).*")
                    ? new WeightContext(SpaceScope.UNSPECIFIED, weightStart)
                    : null;
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
                .replaceFirst("^\\s*공식\\s*안내\\s*[·•:\\-]?\\s*", "")
                .replaceAll("^[\\s,.;:!?/()\\[\\]{}·ㆍ•\\-]+", "")
                .replaceAll("[\\s,.;:!?/()\\[\\]{}·ㆍ•\\-]+$", "")
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
                || "실외".equals(normalized)
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
        private final List<ConfirmationCondition> confirmationConditions = new ArrayList<>();
        private final List<FeeRule> feeRules = new ArrayList<>();
        private final List<UnparsedFragment> unparsedFragments = new ArrayList<>();

        // 한 원문 필드의 처리가 끝난 뒤 합쳐, 오류가 난 필드의 일부 규칙만 남지 않게 한다.
        private void merge(ParseResult sourceResult) {
            weightLimits.addAll(sourceResult.weightLimits);
            sizeConditions.addAll(sourceResult.sizeConditions);
            spaceRules.addAll(sourceResult.spaceRules);
            requirements.addAll(sourceResult.requirements);
            breedRules.addAll(sourceResult.breedRules);
            confirmationConditions.addAll(sourceResult.confirmationConditions);
            feeRules.addAll(sourceResult.feeRules);
            unparsedFragments.addAll(sourceResult.unparsedFragments);
        }

        private int parsedConditionCount() {
            return weightLimits.size()
                    + sizeConditions.size()
                    + spaceRules.size()
                    + requirements.size()
                    + breedRules.size()
                    + confirmationConditions.size()
                    + feeRules.size();
        }

        private PetRule toPetRule() {
            return new PetRule(
                    sources,
                    weightLimits,
                    sizeConditions,
                    spaceRules,
                    requirements,
                    breedRules,
                    confirmationConditions,
                    feeRules,
                    unparsedFragments
            );
        }
    }
}
