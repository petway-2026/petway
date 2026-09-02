package com.petway.hospital.service;

import com.petway.hospital.client.AnimalHospitalClient;
import com.petway.hospital.client.dto.AnimalHospitalApiResponse;
import com.petway.hospital.coordinate.HospitalCoordinateConverter;
import com.petway.hospital.coordinate.Wgs84Coordinate;
import com.petway.hospital.dto.HospitalPageResponse;
import com.petway.hospital.dto.HospitalResponse;
import com.petway.facility.PetFacility;
import com.petway.facility.PetFacilityCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Service
public class HospitalService {

    private static final String ACTIVE_STATUS_CODE = "01";
    private static final Logger log = LoggerFactory.getLogger(HospitalService.class);

    private final AnimalHospitalClient animalHospitalClient;
    private final HospitalCoordinateConverter coordinateConverter;
    private final PetFacilityCatalog facilityCatalog;

    @Autowired
    public HospitalService(AnimalHospitalClient animalHospitalClient, HospitalCoordinateConverter coordinateConverter,
                           PetFacilityCatalog facilityCatalog) {
        this.animalHospitalClient = animalHospitalClient;
        this.coordinateConverter = coordinateConverter;
        this.facilityCatalog = facilityCatalog;
    }

    public HospitalService(AnimalHospitalClient animalHospitalClient, HospitalCoordinateConverter coordinateConverter) {
        this(animalHospitalClient, coordinateConverter, new PetFacilityCatalog());
    }

    public HospitalPageResponse findHospitals(
            int page,
            int size,
            String address,
            String name
    ) {
        List<PetFacility> csvAll = facilityCatalog.hospitals(address, name);
        AnimalHospitalApiResponse.Body body;
        try {
            body = animalHospitalClient.findActiveHospitals(page, size, address, name);
        } catch (RuntimeException exception) {
            log.warn("Animal hospital API failed; returning CSV catalog instead: {}", exception.getMessage());
            return csvResponse(csvAll, page, size);
        }

        List<HospitalResponse> external = body.itemList()
                .stream()
                .filter(item -> ACTIVE_STATUS_CODE.equals(
                        item.businessStatusCode()
                ))
                .map(this::toResponse)
                .toList();
        java.util.Set<String> matchedCsvIds = external.stream()
                .map(h -> facilityCatalog.matchingHospital(h.name(), h.address(), h.latitude(), h.longitude()).orElse(null))
                .filter(java.util.Objects::nonNull).map(PetFacility::id).collect(java.util.stream.Collectors.toSet());

        int from = Math.min((page - 1) * size, csvAll.size());
        int to = Math.min(from + size, csvAll.size());
        List<HospitalResponse> csv = csvAll.subList(from, to).stream()
                .filter(f -> !matchedCsvIds.contains(f.id()))
                .map(this::toResponse).toList();
        List<HospitalResponse> hospitals = new java.util.ArrayList<>(external);
        hospitals.addAll(csv);

        return new HospitalPageResponse(
                hospitals,
                body.pageNo(),
                body.numOfRows(),
                body.totalCount() + csvAll.size()
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

        PetFacility metadata = facilityCatalog.matchingHospital(item.businessName(),
                firstNonBlank(item.roadAddress(), item.lotNumberAddress()),
                coordinate.map(Wgs84Coordinate::latitude).orElse(null), coordinate.map(Wgs84Coordinate::longitude).orElse(null)).orElse(null);
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
                coordinate.map(Wgs84Coordinate::longitude).orElse(null),
                metadata == null ? null : metadata.operatingHours(), metadata == null ? null : metadata.closedDays(),
                metadata == null ? null : metadata.homepage(), metadata == null ? null : metadata.parking(),
                metadata == null ? null : metadata.description(), metadata == null ? "행정안전부" : "행정안전부 · 한국문화정보원"
        );
    }

    public HospitalPageResponse findMedicalFacilities(int page, int size, String address, String name, String facilityType) {
        return csvResponse(facilityCatalog.medicalFacilities(facilityType, address, name), page, size);
    }

    private HospitalPageResponse csvResponse(List<PetFacility> all, int page, int size) {
        int from = Math.min((page - 1) * size, all.size());
        int to = Math.min(from + size, all.size());
        return new HospitalPageResponse(all.subList(from, to).stream().map(this::toResponse).toList(), page, size, all.size());
    }

    private HospitalResponse toResponse(PetFacility f) {
        return new HospitalResponse(f.id(), f.name(), f.address(), f.roadAddress(), f.lotAddress(), f.telephone(),
                "영업 여부 확인 필요", "CSV 등록 시설", null, f.updatedAt(),
                f.latitude() == null ? null : f.latitude().doubleValue(), f.longitude() == null ? null : f.longitude().doubleValue(),
                f.operatingHours(), f.closedDays(), f.homepage(), f.parking(), f.description(), "한국문화정보원");
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
