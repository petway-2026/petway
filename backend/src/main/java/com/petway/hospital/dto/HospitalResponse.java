/* 병원·약국의 표시 정보, 위치, 출처를 클라이언트에 전달한다. */
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
        Double longitude,
        String operatingHours,
        String closedDays,
        String homepage,
        String parking,
        String description,
        String source
) {
    public HospitalResponse(String id, String name, String address, String roadAddress, String lotNumberAddress,
                            String telephone, String businessStatus, String detailedStatus, String licenseDate,
                            String dataUpdatedAt, Double latitude, Double longitude) {
        this(id, name, address, roadAddress, lotNumberAddress, telephone, businessStatus, detailedStatus,
                licenseDate, dataUpdatedAt, latitude, longitude, null, null, null, null, null, "행정안전부");
    }
}
