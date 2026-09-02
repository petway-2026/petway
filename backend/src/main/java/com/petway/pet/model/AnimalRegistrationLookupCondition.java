package com.petway.pet.model;

public record AnimalRegistrationLookupCondition(
        String animalRegistrationNumber,
        String rfidCode,
        String ownerName,
        String ownerBirthDate
) {
}
