/* 장소의 공식 동반 원문과 PetWay 판정·근거·준비사항을 구분해 반환한다. */
package com.petway.place.dto;

import com.petway.compatibility.CompatibilityResult;
import com.petway.compatibility.CompatibilityStatus;
import com.petway.petrule.PetRule.Requirement;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRule.SpaceScope;

import java.util.List;

public record PlaceCompatibilityResponse(
        String contentId,
        Long petId,
        String petName,
        PlacePetDetailResponse officialInformation,
        PetwayAssessment petwayAssessment
) {

    public static PlaceCompatibilityResponse from(
            String contentId,
            Long petId,
            String petName,
            PlacePetDetailResponse officialInformation,
            CompatibilityResult result
    ) {
        return new PlaceCompatibilityResponse(
                contentId,
                petId,
                petName,
                officialInformation,
                PetwayAssessment.from(result)
        );
    }

    public record PetwayAssessment(
            CompatibilityStatus status,
            List<String> reasons,
            List<RequirementItem> requirements,
            boolean needsConfirmation,
            CompatibilityResult.ReasonGroups reasonGroups
    ) {
        private static PetwayAssessment from(CompatibilityResult result) {
            return new PetwayAssessment(
                    result.status(),
                    result.reasons(),
                    result.requirements().stream()
                            .map(RequirementItem::from)
                            .distinct()
                            .toList(),
                    result.status() == CompatibilityStatus.NEED_CONFIRMATION,
                    result.reasonGroups()
            );
        }

        public PetwayAssessment {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            requirements = requirements == null
                    ? List.of()
                    : List.copyOf(requirements);
        }

        public PetwayAssessment(CompatibilityStatus status, List<String> reasons,
                                List<RequirementItem> requirements, boolean needsConfirmation) {
            this(status, reasons, requirements, needsConfirmation, null);
        }
    }

    public record RequirementItem(
            RequirementType type,
            String label,
            SpaceScope scope
    ) {
        private static RequirementItem from(Requirement requirement) {
            return new RequirementItem(
                    requirement.type(),
                    label(requirement.type()),
                    requirement.scope()
            );
        }

        private static String label(RequirementType type) {
            return switch (type) {
                case LEASH -> "목줄";
                case MUZZLE -> "입마개";
                case WASTE_BAG -> "배변봉투";
                case WASTE_DISPOSAL -> "배변 처리";
                case CARRIER -> "이동장";
                case STROLLER -> "유모차";
                case MANNER_BELT -> "매너벨트";
                case VACCINATION_STATUS -> "예방접종 완료";
                case VACCINATION_CERTIFICATE -> "예방접종 증명서";
            };
        }
    }
}
