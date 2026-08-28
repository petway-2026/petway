package com.petway.external.weather.mapper;

import org.springframework.stereotype.Component;

@Component
public class KmaGridConverter {

    private static final double EARTH_RADIUS = 6371.00877;
    private static final double GRID_SIZE = 5.0;
    private static final double STANDARD_LATITUDE_1 = 30.0;
    private static final double STANDARD_LATITUDE_2 = 60.0;
    private static final double ORIGIN_LONGITUDE = 126.0;
    private static final double ORIGIN_LATITUDE = 38.0;
    private static final double ORIGIN_X = 43.0;
    private static final double ORIGIN_Y = 136.0;

    public GridPoint convert(double latitude, double longitude) {
        double radius = EARTH_RADIUS / GRID_SIZE;
        double radians = Math.PI / 180.0;
        double slat1 = STANDARD_LATITUDE_1 * radians;
        double slat2 = STANDARD_LATITUDE_2 * radians;
        double olon = ORIGIN_LONGITUDE * radians;
        double olat = ORIGIN_LATITUDE * radians;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = radius * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + latitude * radians * 0.5);
        ra = radius * sf / Math.pow(ra, sn);
        double theta = longitude * radians - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int x = (int) Math.floor(ra * Math.sin(theta) + ORIGIN_X + 0.5);
        int y = (int) Math.floor(ro - ra * Math.cos(theta) + ORIGIN_Y + 0.5);
        return new GridPoint(x, y);
    }

    public record GridPoint(int nx, int ny) {
    }
}
