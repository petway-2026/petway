package com.petway.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotBlank @Pattern(regexp = "PLACE|HOSPITAL|PHARMACY", message = "지원하지 않는 리뷰 대상입니다.") String targetType,
        @NotBlank @Size(max = 150) String targetId,
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.") @Max(value = 5, message = "별점은 5점 이하여야 합니다.") int rating,
        @NotBlank(message = "리뷰 내용을 입력해주세요.") @Size(max = 100, message = "리뷰는 100자 이내로 작성해주세요.") String content
) {}
