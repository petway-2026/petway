package com.petway.hospital.service;

import com.petway.hospital.client.AnimalHospitalClient;
import com.petway.hospital.client.dto.AnimalHospitalApiResponse;
import com.petway.hospital.coordinate.HospitalCoordinateConverter;
import com.petway.hospital.coordinate.Wgs84Coordinate;
import com.petway.hospital.dto.HospitalPageResponse;
import com.petway.hospital.dto.HospitalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private static final String ACTIVE_STATUS_CODE = "01";

    private final AnimalHospitalClient animalHospitalClient;
    private final HospitalCoordinateConverter coordinateConverter;

    public HospitalPageResponse findHospitals(
            int page,
            int size,
            String address,
            String name
    ) {
        AnimalHospitalApiResponse.Body body =
                animalHospitalClient.findActiveHospitals(
                        page,
                        size,
                        address,
                        name
                );

        List<HospitalResponse> hospitals = body.itemList()
                .stream()
                .filter(item -> ACTIVE_STATUS_CODE.equals(
                        item.businessStatusCode()
                ))
                .map(this::toResponse)
                .toList();

        return new HospitalPageResponse(
                hospitals,
                body.pageNo(),
                body.numOfRows(),
                body.totalCount()
        );
    }

    private HospitalResponse toResponse(
            AnimalHospitalApiResponse.Item item
    ) {
        Optional<Wgs84Coordinate> coordinate =
                coordinateConverter.convert(
                        item.coordinateX(),
                        item.coordinateY()
                );

        return new HospitalResponse(
                item.managementNumber(),
                item.businessName(),
                firstNonBlank(
                        item.roadAddress(),
                        item.lotNumberAddress()
                ),
                blankToNull(item.roadAddress()),
                blankToNull(item.lotNumberAddress()),
                blankToNull(item.telephone()),
                item.businessStatusName(),
                item.detailedStatusName(),
                blankToNull(item.licenseDate()),
                blankToNull(item.dataUpdatedAt()),
                coordinate.map(Wgs84Coordinate::latitude).orElse(null),
                coordinate.map(Wgs84Coordinate::longitude).orElse(null)
        );
    }

    private static String firstNonBlank(String first, String second) {
        String normalizedFirst = blankToNull(first);
        return normalizedFirst != null
                ? normalizedFirst
                : blankToNull(second);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
