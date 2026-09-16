/* 반려견 등록에 필요한 프로필과 성향을 받으며 필수값·범위 검증 조건을 선언한다. */
package com.petway.pet.dto;

import com.petway.pet.enums.ActivityLevel;
import com.petway.pet.enums.CrowdTolerance;
import com.petway.pet.enums.PlacePreference;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.enums.SocialityLevel;
import com.petway.pet.enums.DogBreed;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetCreateRequest(

        @NotBlank(message = "반려견 이름은 필수입니다.")
        @Size(
                max = 50,
                message = "반려견 이름은 50자 이하로 입력해주세요."
        )
        String petName,


        @Size(
                max = 100,
                message = "견종은 100자 이하로 입력해주세요."
        )
        String breedDetail,

        DogBreed breed,

        Boolean animalRegistered,


        LocalDate birthDate,


        @NotNull(message = "체중은 필수입니다.")
        @DecimalMin(
                value = "1",
                message = "체중은 0kg보다 커야 합니다."
        )
        @Digits(integer = 3, fraction = 0, message = "체중은 정수로 입력해주세요.")
        BigDecimal weightKg,


        PetSizeType sizeType,


        ActivityLevel activityLevel,

        CrowdTolerance crowdTolerance,

        PlacePreference placePreference,

        SocialityLevel socialityLevel,

        Boolean vaccinated,

        Boolean neutered

) {
}
