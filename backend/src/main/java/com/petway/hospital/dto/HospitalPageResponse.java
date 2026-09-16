/* 병원 검색 목록과 페이지 정보를 함께 반환한다. */
package com.petway.hospital.dto;

import java.util.List;

public record HospitalPageResponse(
        List<HospitalResponse> hospitals,
        int page,
        int size,
        long totalCount
) {
}
