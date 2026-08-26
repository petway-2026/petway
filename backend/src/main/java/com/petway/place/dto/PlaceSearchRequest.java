package com.petway.place.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceSearchRequest {

    @NotBlank(message = "지역 코드는 필수입니다.")
    @Pattern(regexp = "\\d+", message = "지역 코드는 숫자 형식이어야 합니다.")
    private String areaCode;

    @Pattern(regexp = "\\d+", message = "시군구 코드는 숫자 형식이어야 합니다.")
    private String sigunguCode;

    @Pattern(regexp = "\\d+", message = "관광지 유형 코드는 숫자 형식이어야 합니다.")
    private String contentTypeId;

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;
}
