package com.petway.pet.dto;

import com.petway.pet.enums.ActivityLevel;
import com.petway.pet.enums.CrowdTolerance;
import com.petway.pet.enums.PlacePreference;
import com.petway.pet.enums.SocialityLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetUpdateRequest(

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
        String breed,


        LocalDate birthDate,


        @NotNull(message = "체중은 필수입니다.")
        @DecimalMin(
                value = "0.01",
                message = "체중은 0kg보다 커야 합니다."
        )
        BigDecimal weightKg,


        ActivityLevel activityLevel,

        CrowdTolerance crowdTolerance,

        PlacePreference placePreference,

        SocialityLevel socialityLevel,

        Boolean vaccinated,

        Boolean neutered

) {
}