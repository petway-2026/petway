package com.petway.compatibility;

import com.petway.petrule.PetRule.Requirement;

import java.util.List;
import java.util.Objects;

public record CompatibilityResult(
        CompatibilityStatus status,
        List<String> reasons,
        List<Requirement> requirements
) {

    public CompatibilityResult {
        Objects.requireNonNull(status, "status must not be null");
        reasons = reasons == null || reasons.isEmpty()
                ? List.of()
                : List.copyOf(reasons);
        requirements = requirements == null || requirements.isEmpty()
                ? List.of()
                : List.copyOf(requirements);
    }
}
