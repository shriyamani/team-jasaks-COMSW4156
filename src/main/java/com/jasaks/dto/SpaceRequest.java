package com.jasaks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Payload for creating or replacing a space.
 */
public record SpaceRequest(
        @NotBlank(message = "spaceName is required")
        String spaceName,

        @Positive(message = "capacity must be greater than zero")
        int capacity,

        @NotNull(message = "hourlyRate is required")
        @PositiveOrZero(message = "hourlyRate must be zero or greater")
        BigDecimal hourlyRate,

        Set<String> amenities,

        @NotNull(message = "placeId is required")
        Long placeId
) {
    public SpaceRequest {
        amenities = amenities == null ? new HashSet<>() : new HashSet<>(amenities);
    }
}
