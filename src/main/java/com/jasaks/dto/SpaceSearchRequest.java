package com.jasaks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.HashSet;
import java.util.Set;

import java.time.LocalDateTime;

/**
 * Payload for an available space search request.
 *
 * @param placeId identifier of the place to search spaces in
 * @param startTime start time of desired reservation
 * @param endTime end time of desired reservation
 * @param minCapacity minimum number of people the space can hold
 * @param amenities optional amenitities the space must have
 */
public record SpaceSearchRequest(
    Long placeId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    int minCapacity,
    Set<String> amenities
){
    /**
     * Creates a space search request and defensively copies the amenities.
     */
    public SpaceSearchRequest {
        amenities = (amenities == null)
                ? new HashSet<>()
                : new HashSet<>(amenities);
    }

}
