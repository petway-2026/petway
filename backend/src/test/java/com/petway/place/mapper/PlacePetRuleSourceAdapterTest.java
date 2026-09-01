package com.petway.place.mapper;

import com.petway.petrule.PetRule.PetRuleSourceField;
import com.petway.place.model.PlacePetDetail;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlacePetRuleSourceAdapterTest {

    private final PlacePetRuleSourceAdapter adapter =
            new PlacePetRuleSourceAdapter();

    @Test
    void preservesEachOfficialFieldAsItsOwnSource() {
        PlacePetDetail detail = new PlacePetDetail(
                " 전구역 동반가능 ",
                "10kg 이하 동반 가능",
                "목줄 필수",
                "실내 이용은 현장 문의",
                "맹견은 입마개 필수"
        );

        var sources = adapter.adapt(detail);

        assertThat(sources)
                .extracting(source -> source.sourceField())
                .containsExactly(
                        PetRuleSourceField.ACMPY_TYPE_CD,
                        PetRuleSourceField.ACMPY_PSBL_CPAM,
                        PetRuleSourceField.ACMPY_NEED_MTR,
                        PetRuleSourceField.ETC_ACMPY_INFO,
                        PetRuleSourceField.RELA_ACDNT_RISK_MTR
                );
        assertThat(sources.get(0).rawText())
                .isEqualTo(" 전구역 동반가능 ");
    }

    @Test
    void excludesNullAndBlankFieldsWithoutInventingSources() {
        PlacePetDetail detail = new PlacePetDetail(
                null,
                "  ",
                "이동장 필요",
                null,
                ""
        );

        var sources = adapter.adapt(detail);

        assertThat(sources).hasSize(1);
        assertThat(sources.get(0).sourceField())
                .isEqualTo(PetRuleSourceField.ACMPY_NEED_MTR);
        assertThat(sources.get(0).rawText()).isEqualTo("이동장 필요");
    }

    @Test
    void returnsEmptySourcesForMissingDetail() {
        assertThat(adapter.adapt(null)).isEmpty();
    }
}
