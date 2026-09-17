/* PetRuleParser의 동작을 검증하는 테스트다. 검증 대상: 원문 필드의 문맥을 유지하면서 정규식으로 동반 규정을 추출한다. 최종 동반 가능 판정은 하지 않는다. */
package com.petway.petrule;

import com.petway.petrule.PetRule.BreedGroup;
import com.petway.petrule.PetRule.BreedRuleType;
import com.petway.petrule.PetRule.ConfirmationType;
import com.petway.petrule.PetRule.FeeRule;
import com.petway.petrule.PetRule.PetRuleSource;
import com.petway.petrule.PetRule.PetRuleSourceField;
import com.petway.petrule.PetRule.PetSize;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SizeConditionType;
import com.petway.petrule.PetRule.SpaceCondition;
import com.petway.petrule.PetRule.SpaceScope;
import com.petway.petrule.PetRule.SizeCondition;
import com.petway.petrule.PetRule.WeightLimit;
import com.petway.petrule.PetRule.WeightOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
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

    @ParameterizedTest
    @MethodSource("weightCases")
    void parsesRealWeightExpressions(
            String rawText,
            String expectedWeight,
            WeightOperator expectedOperator
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_PSBL_CPAM,
                rawText
        ));

        assertThat(rule.weightLimits()).hasSize(1);
        assertThat(rule.weightLimits().get(0).maxWeightKg())
                .isEqualByComparingTo(expectedWeight);
        assertThat(rule.weightLimits().get(0).operator())
                .isEqualTo(expectedOperator);
        assertThat(rule.weightLimits().get(0).scope())
                .isEqualTo(SpaceScope.UNSPECIFIED);
        assertThat(rule.weightLimits().get(0).source().sourceField())
                .isEqualTo(PetRuleSourceField.ACMPY_PSBL_CPAM);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    private static Stream<Arguments> weightCases() {
        return Stream.of(
                Arguments.of("10kg 이하", "10", WeightOperator.LESS_THAN_OR_EQUAL),
                Arguments.of("10 kg 이하", "10", WeightOperator.LESS_THAN_OR_EQUAL),
                Arguments.of("9kg 이하", "9", WeightOperator.LESS_THAN_OR_EQUAL),
                Arguments.of("20㎏ 미만 동반 가능", "20", WeightOperator.LESS_THAN)
        );
    }

    @Test
    void keepsMultipleWeightLimitsWithoutInformationLoss() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "실내 5kg 이하, 야외 20㎏ 미만"
        ));

        assertThat(rule.weightLimits()).hasSize(2);
        assertThat(rule.weightLimits())
                .extracting(limit -> limit.maxWeightKg().toPlainString())
                .containsExactly("5", "20");
        assertThat(rule.weightLimits())
                .extracting(PetRule.WeightLimit::operator)
                .containsExactly(
                        WeightOperator.LESS_THAN_OR_EQUAL,
                        WeightOperator.LESS_THAN
                );
        assertThat(rule.weightLimits())
                .extracting(PetRule.WeightLimit::scope)
                .containsExactly(SpaceScope.INDOOR, SpaceScope.OUTDOOR);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("areaCases")
    void mapsAcmpyTypeCdByExactValue(
            String rawText,
            SpaceCondition expectedCondition
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_TYPE_CD,
                rawText
        ));

        assertThat(rule.spaceRules())
                .extracting(PetRule.SpaceRule::condition)
                .containsExactly(expectedCondition);
        assertThat(rule.spaceRules().get(0).source().sourceField())
                .isEqualTo(PetRuleSourceField.ACMPY_TYPE_CD);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    private static Stream<Arguments> areaCases() {
        return Stream.of(
                Arguments.of("전구역 동반가능", SpaceCondition.ALL_AREAS_ALLOWED),
                Arguments.of("일부구역 동반가능", SpaceCondition.PARTIAL_AREA_ONLY)
        );
    }

    @ParameterizedTest
    @MethodSource("explicitSpaceCases")
    void parsesOnlyExplicitSpaceScopes(
            String rawText,
            SpaceCondition expectedCondition
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.spaceRules())
                .extracting(PetRule.SpaceRule::condition)
                .containsExactly(expectedCondition);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    private static Stream<Arguments> explicitSpaceCases() {
        return Stream.of(
                Arguments.of("실내 동반 가능", SpaceCondition.INDOOR_ALLOWED),
                Arguments.of("실내 동반 불가", SpaceCondition.INDOOR_PROHIBITED),
                Arguments.of("야외 동반 가능", SpaceCondition.OUTDOOR_ALLOWED),
                Arguments.of("외부 동반 불가", SpaceCondition.OUTDOOR_PROHIBITED),
                Arguments.of("야외 공간만 동반 가능", SpaceCondition.OUTDOOR_ONLY),
                Arguments.of("테라스만 동반 가능", SpaceCondition.TERRACE_ONLY),
                Arguments.of("야외테라스만", SpaceCondition.TERRACE_ONLY)
        );
    }

    @ParameterizedTest
    @MethodSource("unsafePositiveSpaceCases")
    void doesNotCreatePositiveSpaceRuleFromNegativeOrUncertainClause(
            String rawText,
            SpaceCondition forbiddenCondition
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.spaceRules())
                .extracting(PetRule.SpaceRule::condition)
                .doesNotContain(forbiddenCondition);
        assertThat(rule.unparsedFragments())
                .extracting(PetRule.UnparsedFragment::rawText)
                .containsExactly(rawText);
    }

    private static Stream<Arguments> unsafePositiveSpaceCases() {
        return Stream.of(
                Arguments.of(
                        "테라스만 이용 불가",
                        SpaceCondition.TERRACE_ONLY
                ),
                Arguments.of(
                        "야외 공간만 이용 불가",
                        SpaceCondition.OUTDOOR_ONLY
                ),
                Arguments.of(
                        "실내 이용 가능 여부 문의",
                        SpaceCondition.INDOOR_ALLOWED
                ),
                Arguments.of(
                        "전구역 동반가능 여부 문의",
                        SpaceCondition.ALL_AREAS_ALLOWED
                ),
                Arguments.of(
                        "테라스만 이용 여부 문의",
                        SpaceCondition.TERRACE_ONLY
                )
        );
    }

    @Test
    void parsesInquiryWithoutInferringIndoorAllowance() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "실내 시설 이용은 개별 문의 필요"
        ));

        assertThat(rule.spaceRules()).isEmpty();
        assertThat(rule.confirmationConditions())
                .extracting(PetRule.ConfirmationCondition::type)
                .containsExactly(ConfirmationType.INQUIRY_REQUIRED);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("sizeCases")
    void parsesOnlyExplicitlyDeclaredSizes(
            String rawText,
            Set<PetSize> expectedSizes
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_PSBL_CPAM,
                rawText
        ));

        assertThat(rule.sizeConditions()).hasSize(1);
        assertThat(rule.sizeConditions().get(0).type())
                .isEqualTo(SizeConditionType.ALLOWED);
        assertThat(rule.sizeConditions().get(0).sizes())
                .containsExactlyInAnyOrderElementsOf(expectedSizes);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    private static Stream<Arguments> sizeCases() {
        return Stream.of(
                Arguments.of("소형견", Set.of(PetSize.SMALL)),
                Arguments.of("중형견 동반 가능", Set.of(PetSize.MEDIUM)),
                Arguments.of("대형견 허용", Set.of(PetSize.LARGE)),
                Arguments.of(
                        "중소형견",
                        Set.of(PetSize.SMALL, PetSize.MEDIUM)
                )
        );
    }

    @Test
    void doesNotMisreadMediumSmallDogAsSmallOnly() {
        PetRule rule = parser.parse("중소형견");

        assertThat(rule.sizeConditions().get(0).sizes())
                .containsExactlyInAnyOrder(PetSize.SMALL, PetSize.MEDIUM);
        assertThat(rule.sizeConditions()).hasSize(1);
    }

    @Test
    void parsesKtoMediumSmallAbbreviation() {
        PetRule rule = parser.parse("중, 소형견 (10kg 이하)");

        assertThat(rule.sizeConditions()).singleElement()
                .extracting(SizeCondition::sizes)
                .isEqualTo(Set.of(PetSize.SMALL, PetSize.MEDIUM));
        assertThat(rule.weightLimits()).singleElement()
                .extracting(WeightLimit::maxWeightKg)
                .isEqualTo(new BigDecimal("10"));
    }

    @Test
    void parsesExplicitlyProhibitedSize() {
        PetRule rule = parser.parse("대형견 동반 불가");

        assertThat(rule.sizeConditions()).hasSize(1);
        assertThat(rule.sizeConditions().get(0).type())
                .isEqualTo(SizeConditionType.DISALLOWED);
        assertThat(rule.sizeConditions().get(0).sizes())
                .containsExactly(PetSize.LARGE);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void doesNotInferSizeFromWeight() {
        PetRule rule = parser.parse("8kg 이하");

        assertThat(rule.weightLimits()).hasSize(1);
        assertThat(rule.sizeConditions()).isEmpty();
    }

    @Test
    void parsesAllBreedsAllowed() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_PSBL_CPAM,
                "전 견종 동반 가능"
        ));

        assertThat(rule.breedRules())
                .extracting(PetRule.BreedRule::type)
                .containsExactly(BreedRuleType.ALL_BREEDS_ALLOWED);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void parsesCsvAllBreedsAllowedWording() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_PSBL_CPAM,
                "모든 견종 동반 가능"
        ));

        assertThat(rule.breedRules())
                .extracting(PetRule.BreedRule::type)
                .containsExactly(BreedRuleType.ALL_BREEDS_ALLOWED);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "맹견의 경우 입마개 착용 필수",
            "맹견의 경우, 입마개 착용 필수",
            "맹견 의 경우, 입마개 착용 필수",
            "맹견의 경우: 입마개 착용 필수",
            "실내에서 맹견의 경우, 입마개 착용 필수",
            "맹견의 경우, 실내에서 입마개 착용 필수"
    })
    void keepsDangerousBreedAsRequirementTargetInsteadOfBlockingWholePlace(String text) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                text
        ));

        assertThat(rule.requirements()).hasSize(1);
        assertThat(rule.requirements().get(0).type())
                .isEqualTo(RequirementType.MUZZLE);
        assertThat(rule.requirements().get(0).target().breedGroup())
                .isEqualTo(BreedGroup.DANGEROUS_BREEDS);
        assertThat(rule.breedRules()).isEmpty();
        assertThat(rule.sizeConditions()).isEmpty();
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void keepsLargeDogRequirementTargetWithoutCreatingAllowedSize() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                "대형견 입마개 필수"
        ));

        assertThat(rule.requirements()).hasSize(1);
        assertThat(rule.requirements().get(0).target().sizes())
                .containsExactly(PetSize.LARGE);
        assertThat(rule.sizeConditions()).isEmpty();
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void parsesNeedMatterRequirementsWithoutMandatoryWords() {
        PetRule rule = parser.parse(List.of(
                source(PetRuleSourceField.ACMPY_NEED_MTR, "목줄 착용"),
                source(PetRuleSourceField.ACMPY_NEED_MTR, "이동장(켄넬)사용")
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactly(RequirementType.LEASH, RequirementType.CARRIER);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("negatedRequirementCases")
    void doesNotCreateRequirementFromExplicitNegation(
            String rawText,
            RequirementType forbiddenType
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                rawText
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .doesNotContain(forbiddenType);
        assertThat(rule.unparsedFragments())
                .extracting(PetRule.UnparsedFragment::rawText)
                .containsExactly(rawText);
    }

    private static Stream<Arguments> negatedRequirementCases() {
        return Stream.of(
                Arguments.of("입마개 불필요", RequirementType.MUZZLE),
                Arguments.of("목줄 착용 불필요", RequirementType.LEASH),
                Arguments.of("목줄 착용 필요 없음", RequirementType.LEASH),
                Arguments.of("이동장 사용 안 함", RequirementType.CARRIER),
                Arguments.of("배변봉투 필요 없음", RequirementType.WASTE_BAG),
                Arguments.of("입마개 미착용 가능", RequirementType.MUZZLE)
        );
    }

    @Test
    void parsesWasteBagAndWasteDisposalAsSeparateRequirements() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                "배변봉투 지참 및 배변처리 필수"
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactly(
                        RequirementType.WASTE_BAG,
                        RequirementType.WASTE_DISPOSAL
                );
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void classifiesOfficialNoticeAndMaleMannerBeltWithoutUnknownFragments() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "공식 안내 · 실내 동반 가능, 실외 동반 가능, 수컷 매너벨트 필수"
        ));

        assertThat(rule.spaceRules())
                .extracting(PetRule.SpaceRule::condition)
                .containsExactlyInAnyOrder(
                        SpaceCondition.INDOOR_ALLOWED,
                        SpaceCondition.OUTDOOR_ALLOWED
                );
        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactly(RequirementType.MANNER_BELT);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "이동장(켄넬) 안에 반려동물 몸 전체가 모두 들어가야 동반 가능",
            "케이지 내부에 반려견의 전신이 들어가야만 입장 가능"
    })
    void parsesFullBodyCarrierConditionAcrossSourceFields(String text) {
        for (PetRuleSourceField field : List.of(
                PetRuleSourceField.ACMPY_PSBL_CPAM,
                PetRuleSourceField.ACMPY_NEED_MTR,
                PetRuleSourceField.ETC_ACMPY_INFO,
                PetRuleSourceField.UNSPECIFIED)) {
            PetRule rule = parser.parse(source(field, text));

            assertThat(rule.requirements()).extracting(PetRule.Requirement::type)
                    .containsExactly(RequirementType.CARRIER);
            assertThat(rule.requirements().get(0).source().rawText()).isEqualTo(text);
            assertThat(rule.unparsedFragments()).isEmpty();
        }
    }

    @Test
    void consumesOfficialInquiryLeadInSeparatelyFromPreparationNotices() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "공식 안내 · 거리 내 매장은 개별 정책이 상이하므로 개별 문의 필요 - 맹견의 경우, 입마개 착용 필수 - 배변봉투 지참 및 배변처리 필수"
        ));

        assertThat(rule.confirmationConditions())
                .extracting(PetRule.ConfirmationCondition::type)
                .containsExactly(ConfirmationType.INQUIRY_REQUIRED);
        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactlyInAnyOrder(
                        RequirementType.MUZZLE,
                        RequirementType.WASTE_BAG,
                        RequirementType.WASTE_DISPOSAL
                );
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("needMatterRequirementCases")
    void supportsNeedMatterRequirementVocabulary(
            String rawText,
            RequirementType expectedType
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                rawText
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactly(expectedType);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    private static Stream<Arguments> needMatterRequirementCases() {
        return Stream.of(
                Arguments.of("리드줄 사용", RequirementType.LEASH),
                Arguments.of("입마개 착용", RequirementType.MUZZLE),
                Arguments.of("배변봉투 지참", RequirementType.WASTE_BAG),
                Arguments.of("배변처리", RequirementType.WASTE_DISPOSAL),
                Arguments.of("캐리어 사용", RequirementType.CARRIER),
                Arguments.of("켄넬 사용", RequirementType.CARRIER),
                Arguments.of("케이지 사용", RequirementType.CARRIER),
                Arguments.of("유모차 사용", RequirementType.STROLLER),
                Arguments.of("매너벨트 착용", RequirementType.MANNER_BELT)
        );
    }

    @Test
    void doesNotPromoteBareKeywordFromAnotherFieldToRequirement() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.RELA_POSES_FCLTY,
                "이동장"
        ));

        assertThat(rule.requirements()).isEmpty();
        assertThat(rule.unparsedFragments()).hasSize(1);
        assertThat(rule.unparsedFragments().get(0).sourceField())
                .isEqualTo(PetRuleSourceField.RELA_POSES_FCLTY);
        assertThat(rule.unparsedFragments().get(0).rawText())
                .isEqualTo("이동장");
    }

    @Test
    void parsesExplicitRequirementFromAnotherField() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "목줄 착용 필수"
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactly(RequirementType.LEASH);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void preservesRequirementScopeWithoutInferringSpaceAllowance() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                "건물 내 이동 시 케이지 사용"
        ));

        assertThat(rule.requirements()).hasSize(1);
        assertThat(rule.requirements().get(0).type())
                .isEqualTo(RequirementType.CARRIER);
        assertThat(rule.requirements().get(0).scope())
                .isEqualTo(SpaceScope.INDOOR);
        assertThat(rule.spaceRules()).isEmpty();
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void preservesRequirementTargetAndScopeTogether() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                "실내에서 맹견의 경우 입마개 착용 필수"
        ));

        assertThat(rule.requirements()).hasSize(1);
        assertThat(rule.requirements().get(0).type())
                .isEqualTo(RequirementType.MUZZLE);
        assertThat(rule.requirements().get(0).scope())
                .isEqualTo(SpaceScope.INDOOR);
        assertThat(rule.requirements().get(0).target().breedGroup())
                .isEqualTo(BreedGroup.DANGEROUS_BREEDS);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void doesNotTurnUnsupportedFacilityWeightIntoGlobalLimit() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "전체 20kg 미만, 놀이터 15kg 미만"
        ));

        assertThat(rule.weightLimits()).hasSize(1);
        assertThat(rule.weightLimits().get(0).maxWeightKg())
                .isEqualByComparingTo("20");
        assertThat(rule.weightLimits().get(0).scope())
                .isEqualTo(SpaceScope.UNSPECIFIED);
        assertThat(rule.unparsedFragments())
                .extracting(PetRule.UnparsedFragment::rawText)
                .containsExactly("놀이터 15kg 미만");
    }

    @Test
    void keepsSameKeywordFromDifferentSourceFieldsSeparate() {
        PetRule rule = parser.parse(List.of(
                source(PetRuleSourceField.ACMPY_NEED_MTR, "목줄 착용"),
                source(PetRuleSourceField.ETC_ACMPY_INFO, "목줄 착용 필수")
        ));

        assertThat(rule.requirements()).hasSize(2);
        assertThat(rule.requirements())
                .extracting(requirement -> requirement.source().sourceField())
                .containsExactly(
                        PetRuleSourceField.ACMPY_NEED_MTR,
                        PetRuleSourceField.ETC_ACMPY_INFO
                );
        assertThat(rule.requirements())
                .extracting(requirement -> requirement.source().rawText())
                .containsExactly("목줄 착용", "목줄 착용 필수");
    }

    @Test
    void keepsParsedRulesAndOnlyPreservesUnsupportedRemainder() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "9kg 이하, 중성화 필수"
        ));

        assertThat(rule.weightLimits()).hasSize(1);
        assertThat(rule.unparsedFragments()).hasSize(1);
        assertThat(rule.unparsedFragments().get(0).sourceField())
                .isEqualTo(PetRuleSourceField.ETC_ACMPY_INFO);
        assertThat(rule.unparsedFragments().get(0).rawText())
                .isEqualTo("중성화 필수");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "예방접종 필수",
            "예방접종 완료 필수",
            "접종 완료한 반려견만 이용 가능"
    })
    void parsesRequiredVaccinationStatus(String rawText) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactly(RequirementType.VACCINATION_STATUS);
        assertThat(rule.requirements().get(0).source().rawText()).isEqualTo(rawText);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "예방접종 증명서 필수",
            "예방접종 증명서 지참",
            "접종 증명서 제시 필요"
    })
    void parsesVaccinationCertificateAsStatusAndProofRequirements(String rawText) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ACMPY_NEED_MTR,
                rawText
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .containsExactly(
                        RequirementType.VACCINATION_STATUS,
                        RequirementType.VACCINATION_CERTIFICATE
                );
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "예방접종 권장",
            "예방접종 가능",
            "예방접종 불필요",
            "접종 증명서 필요 없음"
    })
    void doesNotInventRequiredVaccinationFromNonMandatoryText(String rawText) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.requirements())
                .extracting(PetRule.Requirement::type)
                .doesNotContain(
                        RequirementType.VACCINATION_STATUS,
                        RequirementType.VACCINATION_CERTIFICATE
                );
    }

    @ParameterizedTest
    @MethodSource("feeAmountCases")
    void parsesExplicitPetFeeAmount(String rawText, String expectedAmount) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.feeRules()).hasSize(1);
        assertThat(rule.feeRules().get(0).amountWon())
                .isEqualByComparingTo(expectedAmount);
        assertThat(rule.feeRules())
                .extracting(FeeRule::source)
                .extracting(PetRuleSource::rawText)
                .containsExactly(rawText);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    private static Stream<Arguments> feeAmountCases() {
        return Stream.of(
                Arguments.of("반려견 추가 요금 5,000원", "5000"),
                Arguments.of("반려동물 동반 시 추가요금 10000원", "10000"),
                Arguments.of("애견 1마리당 7000원", "7000")
        );
    }

    @Test
    void parsesExplicitPetFeeWithoutInventingAmount() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "반려견 추가 요금 발생"
        ));

        assertThat(rule.feeRules()).hasSize(1);
        assertThat(rule.feeRules().get(0).amountWon()).isNull();
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "입장료 5,000원",
            "주차 요금 3,000원",
            "반려견 추가 요금 없음",
            "반려견 추가 요금 발생할 수 있음"
    })
    void doesNotInventPetFeeFromUnrelatedOrUncertainText(String rawText) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.feeRules()).isEmpty();
    }

    @Test
    void preservesCompletelyUnsupportedContent() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.RELA_ACDNT_RISK_MTR,
                "자세한 사항은 현장 문의"
        ));

        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.unparsedFragments()).hasSize(1);
        assertThat(rule.unparsedFragments().get(0).sourceField())
                .isEqualTo(PetRuleSourceField.RELA_ACDNT_RISK_MTR);
        assertThat(rule.unparsedFragments().get(0).rawText())
                .isEqualTo("자세한 사항은 현장 문의");
    }

    @ParameterizedTest
    @MethodSource("confirmationCases")
    void parsesExplicitReservationAndInquiryConditions(
            String rawText,
            ConfirmationType expectedType
    ) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.confirmationConditions())
                .extracting(PetRule.ConfirmationCondition::type)
                .containsExactly(expectedType);
        assertThat(rule.confirmationConditions().get(0).source().rawText())
                .isEqualTo(rawText);
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    private static Stream<Arguments> confirmationCases() {
        return Stream.of(
                Arguments.of("사전 예약 필수", ConfirmationType.RESERVATION_REQUIRED),
                Arguments.of("사전예약 후 이용 가능", ConfirmationType.RESERVATION_REQUIRED),
                Arguments.of("예약이 필요", ConfirmationType.RESERVATION_REQUIRED),
                Arguments.of("사전 문의 필수", ConfirmationType.INQUIRY_REQUIRED),
                Arguments.of("전화 문의 후 이용 가능", ConfirmationType.INQUIRY_REQUIRED),
                Arguments.of("문의가 필요", ConfirmationType.INQUIRY_REQUIRED),
                Arguments.of("개별 문의 필요", ConfirmationType.INQUIRY_REQUIRED)
        );
    }

    @ParameterizedTest
    @MethodSource("ambiguousConfirmationCases")
    void doesNotInventRequiredConfirmationFromAmbiguousText(String rawText) {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                rawText
        ));

        assertThat(rule.confirmationConditions()).isEmpty();
        assertThat(rule.unparsedFragments())
                .extracting(PetRule.UnparsedFragment::rawText)
                .containsExactly(rawText);
    }

    private static Stream<String> ambiguousConfirmationCases() {
        return Stream.of(
                "예약 가능",
                "문의 02-1234-5678",
                "자세한 사항은 현장 문의"
        );
    }

    @Test
    void doesNotInventAConcreteRuleFromGenericAllowanceText() {
        PetRule rule = parser.parse("반려동물 동반 가능");

        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.unparsedFragments())
                .extracting(PetRule.UnparsedFragment::rawText)
                .containsExactly("반려동물 동반 가능");
    }

    @Test
    void treatsBlankInputAsNoInformationInsteadOfUnknown() {
        PetRule rule = parser.parse(source(
                PetRuleSourceField.ETC_ACMPY_INFO,
                "   "
        ));

        assertThat(rule.sources()).isEmpty();
        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.hasUnknownContent()).isFalse();
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void treatsNullInputAsNoInformationInsteadOfUnknown() {
        PetRule rule = parser.parse((String) null);

        assertThat(rule.sources()).isEmpty();
        assertThat(rule.hasParsedCondition()).isFalse();
        assertThat(rule.hasUnknownContent()).isFalse();
        assertThat(rule.unparsedFragments()).isEmpty();
    }

    @Test
    void keepsCanonicalSourcesWithoutCombiningMultipleFields() {
        PetRule single = parser.parse("10kg 이하");
        PetRule multiple = parser.parse(List.of(
                source(PetRuleSourceField.ACMPY_PSBL_CPAM, "10kg 이하"),
                source(PetRuleSourceField.ACMPY_NEED_MTR, "목줄 착용")
        ));

        assertThat(single.sources())
                .extracting(PetRuleSource::rawText)
                .containsExactly("10kg 이하");
        assertThat(multiple.sources()).hasSize(2);
        assertThat(multiple.sources())
                .extracting(PetRuleSource::rawText)
                .containsExactly("10kg 이하", "목줄 착용");
    }

    @Test
    void resultCollectionsAreImmutable() {
        PetRule rule = parser.parse("소형견 동반 가능");

        assertThatThrownBy(() -> rule.sizeConditions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rule.weightLimits().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void failedSourceDoesNotLeavePartialRulesAndOtherSourcesStillParse() {
        PetRule rule = parser.parse(List.of(
                source(PetRuleSourceField.ACMPY_PSBL_CPAM, "5~10kg, 20~10kg"),
                source(PetRuleSourceField.ETC_ACMPY_INFO, "실내 동반 가능, 실외 동반 가능")
        ));

        assertThat(rule.weightLimits()).isEmpty();
        assertThat(rule.spaceRules()).extracting(PetRule.SpaceRule::condition)
                .containsExactlyInAnyOrder(SpaceCondition.INDOOR_ALLOWED, SpaceCondition.OUTDOOR_ALLOWED);
        assertThat(rule.sources()).hasSize(2);
        assertThat(rule.unparsedFragments()).singleElement().satisfies(fragment -> {
            assertThat(fragment.rawText()).isEqualTo("5~10kg, 20~10kg");
            assertThat(fragment.reason()).isEqualTo(PetRule.UnparsedReason.PARSER_ERROR);
        });
    }

    private static PetRuleSource source(
            PetRuleSourceField sourceField,
            String rawText
    ) {
        return new PetRuleSource(sourceField, rawText);
    }
}
