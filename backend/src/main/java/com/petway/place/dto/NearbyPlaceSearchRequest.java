package com.petway.place.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class NearbyPlaceSearchRequest {

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "33", message = "위도 범위를 확인해 주세요.")
    @DecimalMax(value = "39", message = "위도 범위를 확인해 주세요.")
    private BigDecimal latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "124", message = "경도 범위를 확인해 주세요.")
    @DecimalMax(value = "132", message = "경도 범위를 확인해 주세요.")
    private BigDecimal longitude;

    @Min(value = 100, message = "검색 반경은 100m 이상이어야 합니다.")
    @Max(value = 30000, message = "검색 반경은 30km 이하여야 합니다.")
    private int radius = 20000;

    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "조회 개수는 1개 이상이어야 합니다.")
    @Max(value = 100, message = "조회 개수는 100개 이하여야 합니다.")
    private int size = 20;
}
