package com.petway.facility;

import java.math.BigDecimal;

public record PetFacility(
        String id, String name, String category2, String category3,
        String province, String district, String roadAddress, String lotAddress,
        BigDecimal latitude, BigDecimal longitude, String telephone, String homepage,
        String closedDays, String operatingHours, String parking, String price,
        String petAllowed, String petOnly, String allowedPetSize, String restrictions,
        String indoor, String outdoor, String description, String extraFee, String updatedAt
) {
    public String address() { return roadAddress != null ? roadAddress : lotAddress; }
    public boolean isHospital() { return "동물병원".equals(category3); }
    public boolean isPharmacy() { return "동물약국".equals(category3); }
    public boolean isCoursePlace() {
        return "Y".equalsIgnoreCase(petAllowed)
                && switch (category3) {
                    case "여행지", "박물관", "미술관", "문예회관", "펜션", "호텔", "카페", "식당" -> true;
                    default -> false;
                };
    }
    public String contentTypeId() {
        return switch (category3) {
            case "여행지" -> "12";
            case "박물관", "미술관", "문예회관" -> "14";
            case "펜션", "호텔" -> "32";
            case "카페", "식당" -> "39";
            default -> "38";
        };
    }
}
