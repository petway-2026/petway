package com.petway.place.service;

import com.petway.compatibility.CompatibilityEvaluator;
import com.petway.compatibility.CompatibilityResult;
import com.petway.pet.entity.Pet;
import com.petway.pet.repository.PetRepository;
import com.petway.petrule.PetRule;
import com.petway.petrule.PetRuleParser;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.dto.PlacePetDetailResponse;
import com.petway.place.mapper.PlacePetRuleSourceAdapter;
import com.petway.place.model.PlacePetDetail;
import com.petway.place.port.TourPlaceGateway;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceCompatibilityService {

    private final TourPlaceGateway tourPlaceGateway;
    private final PlacePetRuleSourceAdapter sourceAdapter;
    private final PetRuleParser petRuleParser;
    private final CompatibilityEvaluator compatibilityEvaluator;
    private final UserRepository userRepository;
    private final PetRepository petRepository;

    public PlaceCompatibilityResponse getCompatibility(
            String email,
            String contentId
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "회원 정보를 찾을 수 없습니다."
                ));
        Pet primaryPet = getPrimaryPet(user);

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

    private Pet getPrimaryPet(User user) {
        return petRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> primaryPetNotFound(user));
    }

    private IllegalStateException primaryPetNotFound(User user) {
        List<Pet> pets = petRepository.findAllByUserOrderByIsPrimaryDescIdAsc(user);
        if (pets.isEmpty()) {
            return new IllegalStateException(
                    "대표 반려견이 없습니다. 먼저 반려견을 등록해 주세요."
            );
        }
        return new IllegalStateException(
                "대표 반려견 설정을 확인해 주세요."
        );
    }
}
