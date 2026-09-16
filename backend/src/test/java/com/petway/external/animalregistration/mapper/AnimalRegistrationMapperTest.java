/* AnimalRegistrationMapper의 동작을 검증하는 테스트다. 검증 대상: 외부 등록정보를 반려견 입력용 프로필로 변환하고 품종, 생년월일, 중성화 표기를 정규화한다. */
package com.petway.external.animalregistration.mapper;

import com.petway.external.animalregistration.dto.AnimalRegistrationApiResponse;
import com.petway.external.animalregistration.exception.AnimalRegistrationErrorType;
import com.petway.external.animalregistration.exception.AnimalRegistrationException;
import com.petway.pet.enums.DogBreed;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnimalRegistrationMapperTest {

    private final AnimalRegistrationMapper mapper =
            new AnimalRegistrationMapper();

    @Test
    void mapsOfficialResponseFieldsToPetProfile() {
        var profile = mapper.toRegisteredPetProfile(
                response("00", item("20210510", "보리", "말티즈", "Y"))
        );

        assertThat(profile.petName()).isEqualTo("보리");
        assertThat(profile.birthDate()).isEqualTo(LocalDate.of(2021, 5, 10));
        assertThat(profile.breed()).isEqualTo(DogBreed.MALTESE);
        assertThat(profile.breedDetail()).isEqualTo("말티즈");
        assertThat(profile.neutered()).isTrue();
    }

    @Test
    void preservesUnknownBreedAndUnknownOptionalValues() {
        var profile = mapper.toRegisteredPetProfile(
                response("0000", item("확인불가", "콩이", "등록되지 않은 견종", "U"))
        );

        assertThat(profile.birthDate()).isNull();
        assertThat(profile.breed()).isNull();
        assertThat(profile.breedDetail()).isEqualTo("등록되지 않은 견종");
        assertThat(profile.neutered()).isNull();
    }

    @Test
    void mapsEnglishEnumStyleBreedWithoutLosingUnderscores() {
        var profile = mapper.toRegisteredPetProfile(
                response("00", item("2021-05-10", "보리", "golden retriever", "N"))
        );

        assertThat(profile.breed()).isEqualTo(DogBreed.GOLDEN_RETRIEVER);
        assertThat(profile.neutered()).isFalse();
    }

    @Test
    void treatsMissingItemAsNotFound() {
        assertThatThrownBy(() -> mapper.toRegisteredPetProfile(
                response("00", null)
        ))
                .isInstanceOf(AnimalRegistrationException.class)
                .extracting(error -> ((AnimalRegistrationException) error).getErrorType())
                .isEqualTo(AnimalRegistrationErrorType.NOT_FOUND);
    }

    @Test
    void rejectsProviderFailureAndMalformedSuccessResponse() {
        assertThatThrownBy(() -> mapper.toRegisteredPetProfile(
                response("10", null)
        ))
                .isInstanceOf(AnimalRegistrationException.class)
                .extracting(error -> ((AnimalRegistrationException) error).getErrorType())
                .isEqualTo(AnimalRegistrationErrorType.API_ERROR);

        assertThatThrownBy(() -> mapper.toRegisteredPetProfile(
                response("00", item("20210510", " ", "말티즈", "Y"))
        ))
                .isInstanceOf(AnimalRegistrationException.class)
                .extracting(error -> ((AnimalRegistrationException) error).getErrorType())
                .isEqualTo(AnimalRegistrationErrorType.INVALID_RESPONSE);
    }

    private AnimalRegistrationApiResponse response(
            String resultCode,
            AnimalRegistrationApiResponse.Item item
    ) {
        return new AnimalRegistrationApiResponse(
                new AnimalRegistrationApiResponse.Response(
                        new AnimalRegistrationApiResponse.Header(
                                "request-number",
                                resultCode,
                                "OK",
                                null
                        ),
                        new AnimalRegistrationApiResponse.Body(item)
                )
        );
    }

    private AnimalRegistrationApiResponse.Item item(
            String birthDate,
            String name,
            String breed,
            String neutered
    ) {
        return new AnimalRegistrationApiResponse.Item(
                null,
                null,
                "410123456789012",
                "900123456789012",
                "Y",
                birthDate,
                name,
                "수컷",
                breed,
                neutered,
                "관할기관",
                "20210510",
                "20210511"
        );
    }
}
