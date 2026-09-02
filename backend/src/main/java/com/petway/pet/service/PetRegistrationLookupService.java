package com.petway.pet.service;

import com.petway.pet.dto.PetRegistrationLookupRequest;
import com.petway.pet.dto.PetRegistrationLookupResponse;
import com.petway.pet.model.AnimalRegistrationLookupCondition;
import com.petway.pet.port.AnimalRegistrationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PetRegistrationLookupService {

    private final AnimalRegistrationGateway animalRegistrationGateway;

    public PetRegistrationLookupResponse lookup(PetRegistrationLookupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("동물등록정보 조회 조건은 필수입니다.");
        }

        String registrationNumber = trimToNull(request.animalRegistrationNumber());
        String rfidCode = trimToNull(request.rfidCode());
        String ownerName = trimToNull(request.ownerName());
        String ownerBirthDate = trimToNull(request.ownerBirthDate());

        requireExactlyOne(
                registrationNumber,
                rfidCode,
                "동물등록번호와 RFID 중 하나만 입력해주세요."
        );
        requireExactlyOne(
                ownerName,
                ownerBirthDate,
                "소유자명과 소유자 생년월일 중 하나만 입력해주세요."
        );

        return PetRegistrationLookupResponse.from(
                animalRegistrationGateway.lookup(
                        new AnimalRegistrationLookupCondition(
                                registrationNumber,
                                rfidCode,
                                ownerName,
                                ownerBirthDate
                        )
                )
        );
    }

    private void requireExactlyOne(String first, String second, String message) {
        if ((first == null) == (second == null)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
