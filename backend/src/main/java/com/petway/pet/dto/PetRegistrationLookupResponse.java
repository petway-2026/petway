/* 외부 등록정보 중 반려견 폼 자동 입력에 필요한 값만 반환한다. */
package com.petway.pet.dto;

import com.petway.pet.enums.DogBreed;
import com.petway.pet.model.RegisteredPetProfile;

import java.time.LocalDate;

public record PetRegistrationLookupResponse(
        String petName,
        LocalDate birthDate,
        DogBreed breed,
        String breedDetail,
        Boolean neutered,
        boolean animalRegistered
) {
    public static PetRegistrationLookupResponse from(RegisteredPetProfile profile) {
        return new PetRegistrationLookupResponse(
                profile.petName(),
                profile.birthDate(),
                profile.breed(),
                profile.breedDetail(),
                profile.neutered(),
                true
        );
    }
}
