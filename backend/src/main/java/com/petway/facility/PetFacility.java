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

    public String normalizedAccompanimentType() {
        String value = normalized(petOnly);
        if (value == null) return null;
        if (value.contains("모두") || value.contains("개")) return "모든 견종 동반 가능";
        return value;
    }

    public String normalizedAllowedCapacity() {
        String value = normalized(allowedPetSize);
        if (value == null) return null;
        if (value.contains("모두") || (value.contains("개") && !value.contains("불가"))) {
            return "모든 견종 동반 가능";
        }
        if (value.contains("고양이 전용")
                || (value.matches(".*(고양이|조류|파충류|특수동물|물고기).*" ) && !value.contains("개"))) {
            return "소형견 입장 불가, 중형견 입장 불가, 대형견 입장 불가";
        }
        java.util.regex.Matcher range = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*~\\s*(\\d+(?:\\.\\d+)?)\\s*kg", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(value);
        if (range.find()) return range.group(1) + "kg~" + range.group(2) + "kg";
        if (value.matches("소형/중형")) return "소형견 동반 가능, 중형견 동반 가능";
        if (value.matches("소형|중형|대형|소형견|중형견|대형견")) return value.replace("견", "") + "견 동반 가능";
        return value;
    }

    public String normalizedAdditionalInformation() {
        StringBuilder value = new StringBuilder();
        if ("Y".equalsIgnoreCase(indoor)) append(value, "실내 동반 가능");
        if ("Y".equalsIgnoreCase(outdoor)) append(value, "실외 동반 가능");
        append(value, cleanDelimited(restrictions));
        return value.isEmpty() ? null : value.toString();
    }

    private static String normalized(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        return clean.equalsIgnoreCase("<unset>")
                || clean.equalsIgnoreCase("false")
                || clean.equals("해당없음")
                || clean.equals("정보없음") ? null : clean;
    }
    private static void append(StringBuilder target, String value) { if (value != null && !value.isBlank()) { if (!target.isEmpty()) target.append(", "); target.append(value); } }
    private static String cleanDelimited(String value) {
        if (value == null || value.isBlank()) return null;
        StringBuilder result = new StringBuilder();
        for (String token : value.split(",")) {
            String clean = token.trim();
            if (clean.isEmpty() || clean.equalsIgnoreCase("false") || clean.equalsIgnoreCase("<unset>")) continue;
            if (clean.equals("없음") || clean.equals("해당없음") || clean.equals("정보없음")
                    || clean.equals("제한사항 없음") || clean.equals("제한 없음")) continue;
            if (clean.contains("루프탑 외 입장 불가")) clean = "테라스만 입장 가능";
            if (clean.equals("마당")) clean = "실외만 입장 가능";
            if (!result.isEmpty()) result.append(", ");
            result.append(clean);
        }
        return result.isEmpty() ? null : result.toString();
    }
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
