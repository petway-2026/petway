package com.petway.hospital.dto;

public record HospitalResponse(
        String id,
        String name,
        String address,
        String roadAddress,
        String lotNumberAddress,
        String telephone,
        String businessStatus,
        String detailedStatus,
        String licenseDate,
        String dataUpdatedAt,
        Double latitude,
        Double longitude
) {
}
