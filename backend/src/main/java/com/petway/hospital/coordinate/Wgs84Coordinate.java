/* 지도에 표시할 병원의 WGS84 위도와 경도를 묶는다. */
package com.petway.hospital.coordinate;

public record Wgs84Coordinate(
        double latitude,
        double longitude
) {
}
