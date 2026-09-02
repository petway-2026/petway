package com.petway.facility;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class PetFacilityCatalog {
    private static final Logger log = LoggerFactory.getLogger(PetFacilityCatalog.class);
    private static final String RESOURCE = "한국문화정보원_전국 반려동물 동반 가능 문화시설 위치 데이터_20250324.csv";
    private static final Map<String, String> AREA_CODES = Map.ofEntries(
            Map.entry("서울특별시", "1"), Map.entry("인천광역시", "2"), Map.entry("대전광역시", "3"),
            Map.entry("대구광역시", "4"), Map.entry("광주광역시", "5"), Map.entry("부산광역시", "6"),
            Map.entry("울산광역시", "7"), Map.entry("세종특별자치시", "8"), Map.entry("경기도", "31"),
            Map.entry("강원특별자치도", "32"), Map.entry("강원도", "32"), Map.entry("충청북도", "33"),
            Map.entry("충청남도", "34"), Map.entry("경상북도", "35"), Map.entry("경상남도", "36"),
            Map.entry("전북특별자치도", "37"), Map.entry("전라북도", "37"), Map.entry("전라남도", "38"),
            Map.entry("제주특별자치도", "39")
    );
    private List<PetFacility> places = List.of();
    private List<PetFacility> hospitals = List.of();
    private List<PetFacility> pharmacies = List.of();
    private Map<String, PetFacility> byId = Map.of();

    @PostConstruct
    void load() {
        List<PetFacility> loadedPlaces = new ArrayList<>();
        List<PetFacility> loadedHospitals = new ArrayList<>();
        List<PetFacility> loadedPharmacies = new ArrayList<>();
        Map<String, PetFacility> loadedById = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8), 64 * 1024)) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> c = parse(line);
                if (c.size() < 31) continue;
                PetFacility facility = toFacility(c);
                if (facility.isCoursePlace()) loadedPlaces.add(facility);
                if (facility.isHospital()) loadedHospitals.add(facility);
                if (facility.isPharmacy()) loadedPharmacies.add(facility);
                if (facility.isCoursePlace() || facility.isHospital() || facility.isPharmacy()) loadedById.put(facility.id(), facility);
            }
            places = List.copyOf(loadedPlaces);
            hospitals = List.copyOf(loadedHospitals);
            pharmacies = List.copyOf(loadedPharmacies);
            byId = Map.copyOf(loadedById);
            log.info("Loaded pet facility CSV: places={}, hospitals={}, pharmacies={}", places.size(), hospitals.size(), pharmacies.size());
        } catch (IOException exception) {
            log.error("Failed to load pet facility CSV", exception);
        }
    }

    public List<PetFacility> placesByArea(String areaCode, String contentTypeId) {
        return places.stream().filter(f -> Objects.equals(areaCode, AREA_CODES.get(f.province())))
                .filter(f -> contentTypeId == null || contentTypeId.isBlank() || contentTypeId.equals(f.contentTypeId())).toList();
    }

    public List<PetFacility> placesNearby(BigDecimal latitude, BigDecimal longitude, int radius) {
        if (latitude == null || longitude == null) return List.of();
        double lat = latitude.doubleValue(), lon = longitude.doubleValue();
        return places.stream().filter(f -> f.latitude() != null && f.longitude() != null)
                .filter(f -> distanceMeters(lat, lon, f.latitude().doubleValue(), f.longitude().doubleValue()) <= radius)
                .sorted(Comparator.comparingDouble(f -> distanceMeters(lat, lon, f.latitude().doubleValue(), f.longitude().doubleValue())))
                .toList();
    }

    public List<PetFacility> hospitals(String address, String name) {
        String a = normalize(address), n = normalize(name);
        return hospitals.stream()
                .filter(f -> a.isEmpty() || normalize(f.address()).contains(a))
                .filter(f -> n.isEmpty() || normalize(f.name()).contains(n)).toList();
    }

    public List<PetFacility> medicalFacilities(String facilityType, String address, String name) {
        List<PetFacility> source = "PHARMACY".equalsIgnoreCase(facilityType) ? pharmacies : hospitals;
        String a = normalize(address), n = normalize(name);
        return source.stream().filter(f -> a.isEmpty() || normalize(f.address()).contains(a))
                .filter(f -> n.isEmpty() || normalize(f.name()).contains(n)).toList();
    }

    public Optional<PetFacility> byId(String id) { return Optional.ofNullable(byId.get(id)); }

    public Optional<PetFacility> matching(String name, String address, BigDecimal lat, BigDecimal lon) {
        String normalizedName = normalize(name);
        return places.stream().filter(f -> normalize(f.name()).equals(normalizedName))
                .filter(f -> sameLocation(f, address, lat, lon)).findFirst();
    }

    public Optional<PetFacility> matchingHospital(String name, String address, Double lat, Double lon) {
        String normalizedName = normalize(name);
        return hospitals.stream().filter(f -> normalize(f.name()).equals(normalizedName))
                .filter(f -> sameLocation(f, address, lat == null ? null : BigDecimal.valueOf(lat), lon == null ? null : BigDecimal.valueOf(lon)))
                .findFirst();
    }

    private boolean sameLocation(PetFacility f, String address, BigDecimal lat, BigDecimal lon) {
        String left = normalizeAddress(f.address()), right = normalizeAddress(address);
        if (!left.isEmpty() && !right.isEmpty() && (left.contains(right) || right.contains(left))) return true;
        return lat != null && lon != null && f.latitude() != null && f.longitude() != null
                && distanceMeters(lat.doubleValue(), lon.doubleValue(), f.latitude().doubleValue(), f.longitude().doubleValue()) <= 100;
    }

    private PetFacility toFacility(List<String> c) {
        String id = "csv-" + hash(normalize(c.get(0)) + "|" + normalizeAddress(first(c.get(14), c.get(15))) + "|" + c.get(11) + "|" + c.get(12));
        return new PetFacility(id, clean(c.get(0)), clean(c.get(2)), clean(c.get(3)), clean(c.get(4)), clean(c.get(5)),
                clean(c.get(14)), clean(c.get(15)), decimal(c.get(11)), decimal(c.get(12)), clean(c.get(16)), clean(c.get(17)),
                clean(c.get(18)), clean(c.get(19)), clean(c.get(20)), clean(c.get(21)), clean(c.get(22)), clean(c.get(23)),
                clean(c.get(24)), clean(c.get(25)), clean(c.get(26)), clean(c.get(27)), clean(c.get(28)), clean(c.get(29)), clean(c.get(30)));
    }

    private static List<String> parse(String line) {
        List<String> fields = new ArrayList<>(31); StringBuilder value = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) { char ch = line.charAt(i);
            if (ch == '"') { if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; } else quoted = !quoted; }
            else if (ch == ',' && !quoted) { fields.add(value.toString()); value.setLength(0); } else value.append(ch);
        }
        fields.add(value.toString()); return fields;
    }
    private static String clean(String v) { if (v == null || v.isBlank() || "정보없음".equals(v.trim())) return null; return v.trim(); }
    private static String first(String a, String b) { return clean(a) != null ? clean(a) : clean(b); }
    private static BigDecimal decimal(String v) { try { return clean(v) == null ? null : new BigDecimal(v.trim()); } catch (NumberFormatException e) { return null; } }
    private static String normalize(String v) { return v == null ? "" : v.replaceAll("[^가-힣a-zA-Z0-9]", "").toLowerCase(Locale.ROOT); }
    private static String normalizeAddress(String v) { return normalize(v).replace("번지", ""); }
    private static String hash(String value) { try { byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)); return HexFormat.of().formatHex(bytes, 0, 10); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static double distanceMeters(double a, double o, double b, double p) { double r=6371000, dLat=Math.toRadians(b-a), dLon=Math.toRadians(p-o); double x=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(b))*Math.sin(dLon/2)*Math.sin(dLon/2); return r*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x)); }
}
