/* 출처에 맞게 동반 상세를 얻고 원문 변환, 규정 파싱, 반려견 판정을 순서대로 실행한다. */
package com.petway.place.service;

import com.petway.compatibility.CompatibilityEvaluator;
import com.petway.compatibility.CompatibilityResult;
import com.petway.pet.entity.Pet;
import com.petway.pet.service.PrimaryPetQueryService;
import com.petway.petrule.PetRule;
import com.petway.petrule.PetRuleParser;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.place.mapper.PlacePetRuleSourceAdapter;
import com.petway.place.model.PlacePetDetail;
import com.petway.place.port.TourPlaceGateway;
import com.petway.facility.PetFacilityCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaceCompatibilityService {

    private final TourPlaceGateway tourPlaceGateway;
    private final PlacePetRuleSourceAdapter sourceAdapter;
    private final PetRuleParser petRuleParser;
    private final CompatibilityEvaluator compatibilityEvaluator;
    private final PrimaryPetQueryService primaryPetQueryService;
    private final PetFacilityCatalog facilityCatalog;

    @Autowired
    public PlaceCompatibilityService(TourPlaceGateway tourPlaceGateway, PlacePetRuleSourceAdapter sourceAdapter,
                                     PetRuleParser petRuleParser, CompatibilityEvaluator compatibilityEvaluator,
                                     PrimaryPetQueryService primaryPetQueryService, PetFacilityCatalog facilityCatalog) {
        this.tourPlaceGateway = tourPlaceGateway;
        this.sourceAdapter = sourceAdapter;
        this.petRuleParser = petRuleParser;
        this.compatibilityEvaluator = compatibilityEvaluator;
        this.primaryPetQueryService = primaryPetQueryService;
        this.facilityCatalog = facilityCatalog;
    }

    public PlaceCompatibilityService(TourPlaceGateway tourPlaceGateway, PlacePetRuleSourceAdapter sourceAdapter,
                                     PetRuleParser petRuleParser, CompatibilityEvaluator compatibilityEvaluator,
                                     PrimaryPetQueryService primaryPetQueryService) {
        this(tourPlaceGateway, sourceAdapter, petRuleParser, compatibilityEvaluator, primaryPetQueryService,
                new PetFacilityCatalog());
    }

    public PlaceCompatibilityResponse getCompatibility(
            String email,
            String contentId
    ) {
        return getCompatibility(
                primaryPetQueryService.findByUserEmail(email),
                contentId
        );
    }

    public PlaceCompatibilityResponse getCompatibility(
            Pet primaryPet,
            String contentId
    ) {

        // CSV ID이면 카탈로그 원문을 쓰고 관광공사 ID이면 제공자 상세를 조회한다. 두 출처 모두 같은 판정기로 연결한다.
        PlacePetDetail officialInformation = facilityCatalog.byId(contentId)
                .map(f -> new PlacePetDetail(f.normalizedAccompanimentType(), f.normalizedAllowedCapacity(), null, f.normalizedAdditionalInformation(), null))
                .orElseGet(() -> tourPlaceGateway.getPetDetail(contentId));
        if (officialInformation == null) {
            officialInformation = PlacePetDetail.empty();
        }

        // 원문 필드 보존 → 규정 추출 → 반려견 비교 순서다. 원문과 서비스 해석을 응답에서 구분한다.
        PetRule petRule = petRuleParser.parse(
                sourceAdapter.adapt(officialInformation)
        );
        CompatibilityResult result = compatibilityEvaluator.evaluate(
                primaryPet,
                petRule
        );

        return PlaceCompatibilityResponse.from(
                contentId,
                primaryPet.getId(),
                primaryPet.getPetName(),
                PlacePetDetailResponse.from(officialInformation),
                result
        );
    }

}
