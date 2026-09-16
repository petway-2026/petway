/* PetFacility의 동작을 검증하는 테스트다. 검증 대상: 문화정보원 CSV 한 시설의 정보다. 시설 분류와 동반 안내를 서비스의 공통 표현으로 정규화한다. */
package com.petway.facility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PetFacilityTest {

    @Test
    void buildsCompatibilityTextFromRulesWithoutTreatingDescriptionAsRule() {
        PetFacility facility = facility("동물약국", "제한사항 없음", "Y", "N");

        assertThat(facility.normalizedAllowedCapacity())
                .isEqualTo("모든 견종 동반 가능");
        assertThat(facility.normalizedAdditionalInformation())
                .isEqualTo("실내 동반 가능");
    }

    @Test
    void removesEmptyRestrictionMarkers() {
        assertThat(facility("장소 설명", "없음, 해당없음, 정보없음", "N", "N")
                .normalizedAdditionalInformation()).isNull();
    }

    private PetFacility facility(
            String description,
            String restrictions,
            String indoor,
            String outdoor
    ) {
        return new PetFacility(
                "csv-test", "테스트 장소", "반려의료", "동물약국",
                "서울특별시", "중구", "서울 중구 테스트로 1", null,
                null, null, null, null, null, null,
                null, null,
                "Y", "해당없음", "모두 가능", restrictions,
                indoor, outdoor, description, "없음", "2026-09-04"
        );
    }
}
