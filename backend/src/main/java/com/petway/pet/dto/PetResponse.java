package com.petway.pet.dto;

import com.petway.pet.entity.Pet;
import com.petway.pet.enums.ActivityLevel;
import com.petway.pet.enums.CrowdTolerance;
import com.petway.pet.enums.PlacePreference;
import com.petway.pet.enums.SocialityLevel;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetResponse(

        Long petId,
        String petName,
        String breed,
        LocalDate birthDate,
        BigDecimal weightKg,

        ActivityLevel activityLevel,
        CrowdTolerance crowdTolerance,
        PlacePreference placePreference,
        SocialityLevel socialityLevel,

        Boolean vaccinated,
        Boolean neutered,

        Boolean isPrimary

) {

    public static PetResponse from(
            Pet pet
    ) {

        return new PetResponse(
                pet.getId(),
                pet.getPetName(),
                pet.getBreed(),
                pet.getBirthDate(),
                pet.getWeightKg(),
                pet.getActivityLevel(),
                pet.getCrowdTolerance(),
                pet.getPlacePreference(),
                pet.getSocialityLevel(),
                pet.getVaccinated(),
                pet.getNeutered(),
                pet.getIsPrimary()
        );
    }
}