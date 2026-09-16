/* 사용자의 저장 일정 목록을 조회하고 일정 ID와 소유자를 함께 조건으로 검색한다. */
package com.petway.trip.repository;

import com.petway.trip.entity.SavedTrip;
import com.petway.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedTripRepository extends JpaRepository<SavedTrip, Long> {
    List<SavedTrip> findAllByUserOrderByStartDateDescCreatedAtDesc(User user);
    Optional<SavedTrip> findByIdAndUser(Long id, User user);
}
