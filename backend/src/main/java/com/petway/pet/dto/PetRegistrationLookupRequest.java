package com.petway.pet.dto;

public record PetRegistrationLookupRequest(
        String animalRegistrationNumber,
        String rfidCode,
        String ownerName,
        String ownerBirthDate
) {
}
