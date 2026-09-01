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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceCompatibilityService {

    private final TourPlaceGateway tourPlaceGateway;
    private final PlacePetRuleSourceAdapter sourceAdapter;
    private final PetRuleParser petRuleParser;
    private final CompatibilityEvaluator compatibilityEvaluator;
    private final PrimaryPetQueryService primaryPetQueryService;

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

        PlacePetDetail officialInformation = tourPlaceGateway.getPetDetail(contentId);
        if (officialInformation == null) {
            officialInformation = PlacePetDetail.empty();
        }

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
