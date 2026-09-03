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
