package com.petway.petrule;

import com.petway.petrule.PetRule.PetSize;
import com.petway.petrule.PetRule.Requirement;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SpaceCondition;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.petrule.PetRule.WeightOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetRuleParserTest {

    private PetRuleParser parser;

    @BeforeEach
    void setUp() {
        parser = new PetRuleParser();
    }

    @Test
    void parsesInclusiveMaximumWeight() {
        PetRule rule = parser.parse("10kg 이하");

        assertThat(rule.weightLimit().maxWeightKg())
                .isEqualByComparingTo("10");
        assertThat(rule.weightLimit().operator())
                .isEqualTo(WeightOperator.LESS_THAN_OR_EQUAL);
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    @Test
    void parsesExclusiveMaximumWeight() {
        PetRule rule = parser.parse("10 kg 미만");

        assertThat(rule.weightLimit().maxWeightKg())
                .isEqualByComparingTo("10");
        assertThat(rule.weightLimit().operator())
                .isEqualTo(WeightOperator.LESS_THAN);
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    @Test
    void inclusiveAndExclusiveWeightHaveDifferentBoundaries() {
        PetRule inclusive = parser.parse("10kg 이하");
        PetRule exclusive = parser.parse("10kg 미만");

        assertThat(inclusive.weightLimit().operator())
                .isNotEqualTo(exclusive.weightLimit().operator());
    }

    @ParameterizedTest
    @MethodSource("allowedSizeCases")
    void parsesOnlyExplicitlyDeclaredAllowedSize(
            String rawText,
            PetSize expectedSize
    ) {
        PetRule rule = parser.parse(rawText);

        assertThat(rule.allowedSizes()).containsExactly(expectedSize);
        assertThat(rule.disallowedSizes()).isEmpty();
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    private static Stream<Arguments> allowedSizeCases() {
        return Stream.of(
                Arguments.of("소형견", PetSize.SMALL),
                Arguments.of("중형견 동반 가능", PetSize.MEDIUM),
                Arguments.of("대형견 허용", PetSize.LARGE)
        );
    }

    @Test
    void parsesExplicitlyProhibitedSize() {
        PetRule rule = parser.parse("대형견 동반 불가");

        assertThat(rule.disallowedSizes()).containsExactly(PetSize.LARGE);
        assertThat(rule.allowedSizes()).isEmpty();
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    @Test
    void doesNotInferSizeFromWeight() {
        PetRule rule = parser.parse("8kg 이하");

        assertThat(rule.weightLimit()).isNotNull();
        assertThat(rule.allowedSizes()).isEmpty();
        assertThat(rule.disallowedSizes()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("spaceCases")
    void parsesExplicitSpaceConditions(
            String rawText,
            SpaceCondition expectedCondition
    ) {
        PetRule rule = parser.parse(rawText);

        assertThat(rule.spaceConditions()).containsExactly(expectedCondition);
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    private static Stream<Arguments> spaceCases() {
        return Stream.of(
                Arguments.of("실내 동반 가능", SpaceCondition.INDOOR_ALLOWED),
                Arguments.of("실내 동반 불가", SpaceCondition.INDOOR_PROHIBITED),
                Arguments.of("야외 동반 가능", SpaceCondition.OUTDOOR_ALLOWED),
                Arguments.of("야외 동반 불가", SpaceCondition.OUTDOOR_PROHIBITED),
                Arguments.of("테라스만 동반 가능", SpaceCondition.TERRACE_ONLY),
                Arguments.of("일부 구역만 가능", SpaceCondition.PARTIAL_AREA_ONLY)
        );
    }

    @Test
    void treatsCarrierAndCrateAsTheSameRequirement() {
        Requirement carrier = new Requirement(
                RequirementType.CARRIER,
                SpaceScope.UNSPECIFIED
        );

        assertThat(parser.parse("이동장 필수").requirements())
                .containsExactly(carrier);
        assertThat(parser.parse("캐리어 필요").requirements())
                .containsExactly(carrier);
    }

    @Test
    void preservesExplicitRequirementScopeWithoutInferringSpaceAllowance() {
        PetRule rule = parser.parse("실내 이동장 필수");

        assertThat(rule.requirements()).containsExactly(new Requirement(
                RequirementType.CARRIER,
                SpaceScope.INDOOR
        ));
        assertThat(rule.spaceConditions()).isEmpty();
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    @Test
    void parsesRequiredLeash() {
        PetRule rule = parser.parse("목줄 착용 필수");

        assertThat(rule.requirements()).containsExactly(new Requirement(
                RequirementType.LEASH,
                SpaceScope.UNSPECIFIED
        ));
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    @Test
    void parsesMultipleConditionsFromOneSentence() {
        String rawText = "10kg 이하 소형견은 목줄 착용 시 실내 동반 가능";

        PetRule rule = parser.parse(rawText);

        assertThat(rule.rawText()).isEqualTo(rawText);
        assertThat(rule.weightLimit().maxWeightKg())
                .isEqualByComparingTo("10");
        assertThat(rule.weightLimit().operator())
                .isEqualTo(WeightOperator.LESS_THAN_OR_EQUAL);
        assertThat(rule.allowedSizes()).containsExactly(PetSize.SMALL);
        assertThat(rule.requirements()).containsExactly(new Requirement(
                RequirementType.LEASH,
                SpaceScope.UNSPECIFIED
        ));
        assertThat(rule.spaceConditions())
                .containsExactly(SpaceCondition.INDOOR_ALLOWED);
        assertThat(rule.hasUnknownContent()).isFalse();
    }

    @Test
    void keepsParsedConditionsAndMarksUnsupportedContent() {
        String rawText = "10kg 이하, 예방접종 필수";

        PetRule rule = parser.parse(rawText);

        assertThat(rule.rawText()).isEqualTo(rawText);
        assertThat(rule.weightLimit()).isNotNull();
        assertThat(rule.requirements()).isEmpty();
        assertThat(rule.hasUnknownContent()).isTrue();
    }

    @Test
    void marksFieldInquiryAsUnknownWithoutInventingConditions() {
        String rawText = "자세한 사항은 현장 문의";

        PetRule rule = parser.parse(rawText);

        assertThat(rule.rawText()).isEqualTo(rawText);
        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.hasUnknownContent()).isTrue();
    }

    @Test
    void treatsBlankInputAsUnknownAndPreservesIt() {
        String rawText = "   ";

        PetRule rule = parser.parse(rawText);

        assertThat(rule.rawText()).isEqualTo(rawText);
        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.hasUnknownContent()).isTrue();
    }

    @Test
    void treatsNullInputAsUnknownWithoutThrowing() {
        PetRule rule = parser.parse(null);

        assertThat(rule.rawText()).isNull();
        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.hasUnknownContent()).isTrue();
    }

    @Test
    void doesNotInterpretGenericUnknownSentenceAsPositiveCondition() {
        PetRule rule = parser.parse("반려동물 동반 가능");

        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.hasUnknownContent()).isTrue();
    }

    @Test
    void doesNotParseUnsupportedRules() {
        PetRule rule = parser.parse(
                "2마리 이하, 입마개와 예방접종 필수, 예약 후 이용 가능"
        );

        assertThat(rule.weightLimit()).isNull();
        assertThat(rule.allowedSizes()).isEmpty();
        assertThat(rule.disallowedSizes()).isEmpty();
        assertThat(rule.spaceConditions()).isEmpty();
        assertThat(rule.requirements()).isEmpty();
        assertThat(rule.hasUnknownContent()).isTrue();
    }

    @Test
    void resultCollectionsAreImmutable() {
        PetRule rule = parser.parse("소형견 동반 가능");

        assertThatThrownBy(() -> rule.allowedSizes().add(PetSize.LARGE))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(rule.allowedSizes()).isEqualTo(Set.of(PetSize.SMALL));
    }
}
