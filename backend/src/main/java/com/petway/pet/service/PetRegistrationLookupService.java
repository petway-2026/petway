/* 동물 식별자와 소유자 확인정보를 각각 하나씩 선택했는지 검사한 뒤 외부 조회를 수행한다. */
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

    // 둘 다 비어 있거나 둘 다 입력된 경우를 거부한다. 각 식별자 그룹에서 정확히 하나만 선택해야 한다.
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
