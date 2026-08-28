package com.petway.hospital.controller;

import com.petway.hospital.dto.HospitalPageResponse;
import com.petway.hospital.service.HospitalService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping
    public HospitalPageResponse findHospitals(
            @RequestParam(defaultValue = "1")
            @Min(1)
            int page,
            @RequestParam(defaultValue = "100")
            @Min(1)
            @Max(100)
            int size,
            @RequestParam(required = false)
            @Size(max = 100)
            String address,
            @RequestParam(required = false)
            @Size(max = 100)
            String name
    ) {
        return hospitalService.findHospitals(
                page,
                size,
                address,
                name
        );
    }
}
