/* 동물등록번호 또는 RFID와 소유자 확인정보를 받는 외부 조회 요청이다. */
package com.petway.pet.dto;

public record PetRegistrationLookupRequest(
        String animalRegistrationNumber,
        String rfidCode,
        String ownerName,
        String ownerBirthDate
) {
}
