/* 동반 상세의 각 필드를 원문 출처와 함께 파서 입력으로 변환해 필드별 문맥을 보존한다. */
package com.petway.place.mapper;

import com.petway.petrule.PetRule.PetRuleSource;
import com.petway.petrule.PetRule.PetRuleSourceField;
import com.petway.place.model.PlacePetDetail;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlacePetRuleSourceAdapter {

    public List<PetRuleSource> adapt(PlacePetDetail detail) {
        if (detail == null) {
            return List.of();
        }

        List<PetRuleSource> sources = new ArrayList<>();
        add(sources, PetRuleSourceField.ACMPY_TYPE_CD, detail.accompanimentType());
        add(sources, PetRuleSourceField.ACMPY_PSBL_CPAM, detail.allowedCapacity());
        add(sources, PetRuleSourceField.ACMPY_NEED_MTR, detail.requiredItems());
        add(sources, PetRuleSourceField.ETC_ACMPY_INFO, detail.additionalInformation());
        add(sources, PetRuleSourceField.RELA_ACDNT_RISK_MTR, detail.safetyNotice());
        add(sources, PetRuleSourceField.RELA_POSES_FCLTY, detail.availableFacilities());
        add(sources, PetRuleSourceField.RELA_FRNSH_PRDLST, detail.providedItems());
        add(sources, PetRuleSourceField.RELA_PURC_PRDLST, detail.purchasableItems());
        add(sources, PetRuleSourceField.RELA_RNTL_PRDLST, detail.rentableItems());
        return List.copyOf(sources);
    }

    private void add(
            List<PetRuleSource> sources,
            PetRuleSourceField sourceField,
            String rawText
    ) {
        if (rawText == null || rawText.isBlank()) {
            return;
        }
        sources.add(new PetRuleSource(sourceField, rawText));
    }
}
