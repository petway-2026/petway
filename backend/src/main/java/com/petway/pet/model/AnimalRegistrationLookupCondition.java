/* 검증·정규화한 동물 식별자와 소유자 확인정보를 외부 조회 포트로 전달한다. */
package com.petway.pet.model;

public record AnimalRegistrationLookupCondition(
        String animalRegistrationNumber,
        String rfidCode,
        String ownerName,
        String ownerBirthDate
) {
}
