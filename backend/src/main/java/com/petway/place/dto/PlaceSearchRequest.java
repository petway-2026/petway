package com.petway.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceSearchRequest {

    @Schema(description = "광역 지역 코드", example = "1")
    @NotBlank(message = "지역 코드는 필수입니다.")
    @Pattern(
            regexp = "\\d{1,2}",
            message = "지역 코드는 1~2자리 숫자여야 합니다."
    )
    private String areaCode;

    @Schema(description = "시군구 코드", example = "1", nullable = true)
    @Pattern(
            regexp = "\\d{1,3}",
            message = "시군구 코드는 1~3자리 숫자여야 합니다."
    )
    private String sigunguCode;

    @Schema(description = "관광지 유형 코드", example = "12", nullable = true)
    @Pattern(
            regexp = "\\d{1,2}",
            message = "관광지 유형 코드는 1~2자리 숫자여야 합니다."
    )
    private String contentTypeId;

    @Schema(description = "페이지 번호", example = "1", defaultValue = "1")
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;
}
