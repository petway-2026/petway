/* 여행 소유자와 요약 컬럼을 저장하고 날짜별 상세 일정은 JSON 스냅샷으로 보관한다. */
package com.petway.trip.entity;

import com.petway.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_trip", indexes = @Index(name = "idx_saved_trip_user_created", columnList = "user_id,created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedTrip {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saved_trip_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "region_name", nullable = false, length = 50)
    private String regionName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "transport_mode", nullable = false, length = 20)
    private String transportMode;

    @Column(name = "place_count", nullable = false)
    private int placeCount;

    @Lob
    @Column(name = "itinerary_json", nullable = false, columnDefinition = "LONGTEXT")
    private String itineraryJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static SavedTrip create(User user, String title, String regionName, LocalDate startDate,
                                   LocalDate endDate, String transportMode, int placeCount, String itineraryJson) {
        SavedTrip trip = new SavedTrip();
        trip.user = user;
        trip.title = title;
        trip.regionName = regionName;
        trip.startDate = startDate;
        trip.endDate = endDate;
        trip.transportMode = transportMode;
        trip.placeCount = placeCount;
        trip.itineraryJson = itineraryJson;
        trip.createdAt = LocalDateTime.now();
        return trip;
    }
}
