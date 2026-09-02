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
