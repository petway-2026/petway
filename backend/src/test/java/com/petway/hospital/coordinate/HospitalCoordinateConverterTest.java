/* HospitalCoordinateConverter의 동작을 검증하는 테스트다. 검증 대상: 병원 원본 좌표계를 지도 표시용 WGS84 위도·경도로 변환한다. */
package com.petway.hospital.coordinate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HospitalCoordinateConverterTest {

    private final HospitalCoordinateConverter converter =
            new HospitalCoordinateConverter();

    @Test
    void convertsActualDongtanEpsg5174SampleToWgs84() {
        Wgs84Coordinate coordinate = converter.convert(
                        "207822.739588256",
                        "410789.35102542"
                )
                .orElseThrow();

        assertThat(coordinate.latitude()).isBetween(37.1, 37.3);
        assertThat(coordinate.longitude()).isBetween(127.0, 127.2);
    }

    @Test
    void convertsActualJinanEpsg5174SampleToWgs84() {
        Wgs84Coordinate coordinate = converter.convert(
                        "239339.46747107",
                        "255291.860554085"
                )
                .orElseThrow();

        assertThat(coordinate.latitude()).isBetween(35.7, 35.9);
        assertThat(coordinate.longitude()).isBetween(127.3, 127.6);
    }

    @Test
    void returnsEmptyForMissingOrMalformedCoordinates() {
        assertThat(converter.convert("", "410789.3")).isEmpty();
        assertThat(converter.convert("not-a-number", "410789.3"))
                .isEmpty();
        assertThat(converter.convert(null, null)).isEmpty();
    }
}
