package com.jasaks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Payload for creating or replacing a space.
 *
 * @param spaceName name of the space
 * @param capacity maximum number of people the space holds
 * @param hourlyRate hourly rental rate for the space
 * @param amenities amenities available in the space
 * @param placeId identifier of the place containing the space
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
    /**
     * Creates a request and defensively copies the amenities.
     */
    public SpaceRequest {
        amenities = amenities == null
                ? new HashSet<>()
                : new HashSet<>(amenities.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()));
    }
}
