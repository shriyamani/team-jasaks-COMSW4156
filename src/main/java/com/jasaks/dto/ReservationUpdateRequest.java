package com.jasaks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * Payload for modifying a reservation. Every field except
 * {@code bookerId} is optional; one left out keeps its current value.
 *
 * @param bookerId the booker who holds the reservation
 * @param spaceId the space to move the reservation to, or null to keep it
 * @param startTime the new start time, or null to keep it
 * @param endTime the new end time, or null to keep it
 * @param groupSize the new number of people, or null to keep it
 */
public record ReservationUpdateRequest(
        @NotBlank(message = "bookerId is required")
        String bookerId,

        Long spaceId,

        LocalDateTime startTime,

        LocalDateTime endTime,

        @Positive(message = "groupSize must be greater than zero")
        Integer groupSize
) {
}
