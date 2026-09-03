package com.petway.pet.model;

import com.petway.pet.enums.DogBreed;

import java.time.LocalDate;

public record RegisteredPetProfile(
        String petName,
        LocalDate birthDate,
        DogBreed breed,
        String breedDetail,
        Boolean neutered
) {
}
