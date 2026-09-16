/* 외부 등록정보에서 얻은 이름·생년월일·견종·중성화 정보를 담는다. */
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
