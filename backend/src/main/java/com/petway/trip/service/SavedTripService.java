/* 여행 입력을 검사해 JSON으로 저장하고 사용자 소유권을 기준으로 조회·삭제한다. */
package com.petway.trip.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petway.trip.dto.SavedTripCreateRequest;
import com.petway.trip.dto.SavedTripPayload;
import com.petway.trip.dto.SavedTripResponse;
import com.petway.trip.entity.SavedTrip;
import com.petway.trip.repository.SavedTripRepository;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedTripService {
    private final SavedTripRepository savedTripRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public SavedTripResponse create(String email, SavedTripCreateRequest request) {
        validate(request);
        User user = user(email);
        // 클라이언트의 요약 건수를 신뢰하지 않고 실제 저장할 날짜별 장소 목록에서 전체 개수를 계산한다.
        int placeCount = request.itinerary().days().stream().mapToInt(day -> day.places().size()).sum();
        try {
            SavedTrip saved = savedTripRepository.save(SavedTrip.create(
                    user, request.title().trim(), request.regionName().trim(), request.startDate(), request.endDate(),
                    normalizedMode(request.transportMode()), placeCount, objectMapper.writeValueAsString(request.itinerary())
            ));
            return response(saved);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("여행일정을 저장할 수 없습니다.", exception);
        }
    }

    public List<SavedTripResponse> list(String email) {
        return savedTripRepository.findAllByUserOrderByStartDateDescCreatedAtDesc(user(email)).stream()
                .map(this::response).toList();
    }

    // 일정 ID만 조회하지 않고 로그인 사용자도 조건에 넣어 다른 사용자의 일정 접근을 막는다.
    public SavedTripResponse get(String email, Long id) {
        return response(savedTripRepository.findByIdAndUser(id, user(email))
                .orElseThrow(() -> new IllegalArgumentException("저장한 여행일정을 찾을 수 없습니다.")));
    }

    @Transactional
    public void delete(String email, Long id) {
        SavedTrip trip = savedTripRepository.findByIdAndUser(id, user(email))
                .orElseThrow(() -> new IllegalArgumentException("저장한 여행일정을 찾을 수 없습니다."));
        savedTripRepository.delete(trip);
    }

    // 저장된 JSON을 응답 모델로 복원한다. 현재 원본 장소 정보를 다시 조회하는 동작은 아니다.
    private SavedTripResponse response(SavedTrip trip) {
        try {
            return new SavedTripResponse(trip.getId(), trip.getTitle(), trip.getRegionName(), trip.getStartDate(),
                    trip.getEndDate(), trip.getTransportMode(), trip.getPlaceCount(),
                    objectMapper.readValue(trip.getItineraryJson(), SavedTripPayload.class), trip.getCreatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 여행일정을 읽을 수 없습니다.", exception);
        }
    }

    private void validate(SavedTripCreateRequest request) {
        if (request == null || request.title() == null || request.title().isBlank() || request.title().length() > 100)
            throw new IllegalArgumentException("여행 제목은 1자 이상 100자 이하로 입력해 주세요.");
        if (request.regionName() == null || request.regionName().isBlank() || request.startDate() == null || request.endDate() == null)
            throw new IllegalArgumentException("여행 지역과 날짜는 필수입니다.");
        if (request.endDate().isBefore(request.startDate())) throw new IllegalArgumentException("여행 종료일이 시작일보다 빠릅니다.");
        if (request.itinerary() == null || request.itinerary().days().isEmpty()
                || request.itinerary().days().stream().allMatch(day -> day.places().isEmpty()))
            throw new IllegalArgumentException("저장할 장소가 한 곳 이상 필요합니다.");
    }

    private String normalizedMode(String mode) { return "TRANSIT".equals(mode) ? "TRANSIT" : "DRIVING"; }
    private User user(String email) { return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다.")); }
}
