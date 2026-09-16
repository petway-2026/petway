/* PetRegistrationLookupService의 동작을 검증하는 테스트다. 검증 대상: 동물 식별자와 소유자 확인정보를 각각 하나씩 선택했는지 검사한 뒤 외부 조회를 수행한다. */
package com.petway.pet.service;

import com.petway.pet.dto.PetRegistrationLookupRequest;
import com.petway.pet.enums.DogBreed;
import com.petway.pet.model.AnimalRegistrationLookupCondition;
import com.petway.pet.model.RegisteredPetProfile;
import com.petway.pet.port.AnimalRegistrationGateway;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PetRegistrationLookupServiceTest {

    private final AnimalRegistrationGateway gateway =
            mock(AnimalRegistrationGateway.class);
    private final PetRegistrationLookupService service =
            new PetRegistrationLookupService(gateway);

    @Test
    void looksUpByRegistrationNumberAndOwnerName() {
        AnimalRegistrationLookupCondition condition =
                new AnimalRegistrationLookupCondition(
                        "410123456789012",
                        null,
                        "홍길동",
                        null
                );
        when(gateway.lookup(condition)).thenReturn(
                new RegisteredPetProfile(
                        "보리",
                        LocalDate.of(2021, 5, 10),
                        DogBreed.MALTESE,
                        "말티즈",
                        true
                )
        );

        var response = service.lookup(
                new PetRegistrationLookupRequest(
                        " 410123456789012 ",
                        null,
                        " 홍길동 ",
                        null
                )
        );

        assertThat(response.petName()).isEqualTo("보리");
        assertThat(response.breed()).isEqualTo(DogBreed.MALTESE);
        assertThat(response.animalRegistered()).isTrue();
        verify(gateway).lookup(condition);
    }

    @Test
    void looksUpByRfidAndOwnerBirthDate() {
        AnimalRegistrationLookupCondition condition =
                new AnimalRegistrationLookupCondition(
                        null,
                        "900123456789012",
                        null,
                        "19900101"
                );
        when(gateway.lookup(condition)).thenReturn(
                new RegisteredPetProfile("콩이", null, null, "기타견", null)
        );

        service.lookup(
                new PetRegistrationLookupRequest(
                        null,
                        "900123456789012",
                        null,
                        "19900101"
                )
        );

        verify(gateway).lookup(condition);
    }

    @Test
    void acceptsTheOtherOfficialIdentifierCombinations() {
        when(gateway.lookup(new AnimalRegistrationLookupCondition(
                "410123456789012", null, null, "19900101"
        ))).thenReturn(new RegisteredPetProfile("보리", null, null, null, null));
        when(gateway.lookup(new AnimalRegistrationLookupCondition(
                null, "900123456789012", "홍길동", null
        ))).thenReturn(new RegisteredPetProfile("콩이", null, null, null, null));

        service.lookup(new PetRegistrationLookupRequest(
                "410123456789012", null, null, "19900101"
        ));
        service.lookup(new PetRegistrationLookupRequest(
                null, "900123456789012", "홍길동", null
        ));

        verify(gateway).lookup(new AnimalRegistrationLookupCondition(
                "410123456789012", null, null, "19900101"
        ));
        verify(gateway).lookup(new AnimalRegistrationLookupCondition(
                null, "900123456789012", "홍길동", null
        ));
    }

    @Test
    void rejectsMissingOrDuplicatedAnimalIdentifiers() {
        assertThatThrownBy(() -> service.lookup(
                new PetRegistrationLookupRequest(null, null, "홍길동", null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동물등록번호와 RFID 중 하나만 입력해주세요.");

        assertThatThrownBy(() -> service.lookup(
                new PetRegistrationLookupRequest("4101", "9001", "홍길동", null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동물등록번호와 RFID 중 하나만 입력해주세요.");
    }

    @Test
    void rejectsMissingOrDuplicatedOwnerIdentifiers() {
        assertThatThrownBy(() -> service.lookup(
                new PetRegistrationLookupRequest("4101", null, null, " ")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소유자명과 소유자 생년월일 중 하나만 입력해주세요.");

        assertThatThrownBy(() -> service.lookup(
                new PetRegistrationLookupRequest("4101", null, "홍길동", "19900101")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("소유자명과 소유자 생년월일 중 하나만 입력해주세요.");
    }
}
