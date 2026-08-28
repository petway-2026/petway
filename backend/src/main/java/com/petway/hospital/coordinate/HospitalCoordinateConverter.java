package com.petway.hospital.coordinate;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class HospitalCoordinateConverter {

    private static final double MIN_LATITUDE = 33.0;
    private static final double MAX_LATITUDE = 39.5;
    private static final double MIN_LONGITUDE = 124.0;
    private static final double MAX_LONGITUDE = 132.0;

    private final CoordinateTransform epsg5174ToWgs84;

    public HospitalCoordinateConverter() {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem source =
                crsFactory.createFromName("EPSG:5174");
        CoordinateReferenceSystem target =
                crsFactory.createFromName("EPSG:4326");

        epsg5174ToWgs84 = new CoordinateTransformFactory()
                .createTransform(source, target);
    }

    public Optional<Wgs84Coordinate> convert(
            String coordinateX,
            String coordinateY
    ) {
        if (coordinateX == null
                || coordinateX.isBlank()
                || coordinateY == null
                || coordinateY.isBlank()) {
            return Optional.empty();
        }

        try {
            // Although EPSG metadata names axes northing/easting, the
            // LocalData API's CRD_INFO_X is easting and CRD_INFO_Y northing.
            ProjCoordinate source = new ProjCoordinate(
                    Double.parseDouble(coordinateX),
                    Double.parseDouble(coordinateY)
            );
            ProjCoordinate target = new ProjCoordinate();
            epsg5174ToWgs84.transform(source, target);

            if (!isValidKoreanCoordinate(target.y, target.x)) {
                return Optional.empty();
            }

            return Optional.of(new Wgs84Coordinate(
                    target.y,
                    target.x
            ));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static boolean isValidKoreanCoordinate(
            double latitude,
            double longitude
    ) {
        return Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= MIN_LATITUDE
                && latitude <= MAX_LATITUDE
                && longitude >= MIN_LONGITUDE
                && longitude <= MAX_LONGITUDE;
    }
}
