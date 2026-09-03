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

        PlacePetDetail officialInformation = facilityCatalog.byId(contentId)
                .map(f -> new PlacePetDetail(f.normalizedAccompanimentType(), f.normalizedAllowedCapacity(), null, f.normalizedAdditionalInformation(), null))
                .orElseGet(() -> tourPlaceGateway.getPetDetail(contentId));
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
