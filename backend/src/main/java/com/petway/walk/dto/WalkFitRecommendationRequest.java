/* 산책 추천의 좌표와 날짜 입력을 표현하고 좌표 범위를 검증한다. */
package com.petway.walk.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class WalkFitRecommendationRequest {

    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "33", message = "위도 범위를 확인해 주세요.")
    @DecimalMax(value = "39", message = "위도 범위를 확인해 주세요.")
    private BigDecimal latitude;

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "124", message = "경도 범위를 확인해 주세요.")
    @DecimalMax(value = "132", message = "경도 범위를 확인해 주세요.")
    private BigDecimal longitude;

    @NotNull(message = "산책 날짜는 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate targetDate;
}
