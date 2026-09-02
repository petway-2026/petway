package com.petway.trip.controller;

import com.petway.trip.dto.SavedTripCreateRequest;
import com.petway.trip.dto.SavedTripResponse;
import com.petway.trip.service.SavedTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saved-trips")
@RequiredArgsConstructor
public class SavedTripController {
    private final SavedTripService savedTripService;

    @PostMapping
    public SavedTripResponse create(Authentication authentication, @RequestBody SavedTripCreateRequest request) {
        return savedTripService.create(authentication.getName(), request);
    }

    @GetMapping
    public List<SavedTripResponse> list(Authentication authentication) {
        return savedTripService.list(authentication.getName());
    }

    @GetMapping("/{id}")
    public SavedTripResponse get(Authentication authentication, @PathVariable Long id) {
        return savedTripService.get(authentication.getName(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
        savedTripService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
