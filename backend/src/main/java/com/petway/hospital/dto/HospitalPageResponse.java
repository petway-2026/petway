package com.petway.hospital.dto;

import java.util.List;

public record HospitalPageResponse(
        List<HospitalResponse> hospitals,
        int page,
        int size,
        long totalCount
) {
}
