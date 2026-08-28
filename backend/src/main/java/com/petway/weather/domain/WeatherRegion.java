package com.petway.weather.domain;

public enum WeatherRegion {
    SEOUL_INCHEON_GYEONGGI("서울ㆍ인천ㆍ경기도", "11B00000", "11B10101"),
    GANGWON_YEONGSEO("강원도 영서", "11D10000", "11D10301"),
    GANGWON_YEONGDONG("강원도 영동", "11D20000", "11D20501"),
    DAEJEON_SEJONG_CHUNGNAM("대전ㆍ세종ㆍ충청남도", "11C20000", "11C20401"),
    CHUNGBUK("충청북도", "11C10000", "11C10301"),
    GWANGJU_JEONNAM("광주ㆍ전라남도", "11F20000", "11F20501"),
    JEONBUK("전라북도", "11F10000", "11F10201"),
    DAEGU_GYEONGBUK("대구ㆍ경상북도", "11H10000", "11H10701"),
    BUSAN_ULSAN_GYEONGNAM("부산ㆍ울산ㆍ경상남도", "11H20000", "11H20201"),
    JEJU("제주도", "11G00000", "11G00201");

    private final String displayName;
    private final String landForecastCode;
    private final String temperatureForecastCode;

    WeatherRegion(String displayName, String landForecastCode, String temperatureForecastCode) {
        this.displayName = displayName;
        this.landForecastCode = landForecastCode;
        this.temperatureForecastCode = temperatureForecastCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLandForecastCode() {
        return landForecastCode;
    }

    public String getTemperatureForecastCode() {
        return temperatureForecastCode;
    }
}
