/* KmaGridConverter의 동작을 검증하는 테스트다. 검증 대상: 위도·경도를 기상청 단기예보용 격자 좌표 nx, ny로 변환한다. */
package com.petway.external.weather.mapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmaGridConverterTest {

    private final KmaGridConverter converter = new KmaGridConverter();

    @Test
    void convertsSeoulCityHallCoordinatesToKmaGrid() {
        KmaGridConverter.GridPoint point = converter.convert(37.5665, 126.9780);

        assertThat(point.nx()).isEqualTo(60);
        assertThat(point.ny()).isEqualTo(127);
    }
}
