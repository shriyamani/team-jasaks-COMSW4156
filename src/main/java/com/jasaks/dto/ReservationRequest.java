package com.jasaks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * Payload for creating a reservation. Times are in the place's local
 * time zone, not the server's.
 *
 * @param bookerId the booker making the reservation
 * @param spaceId the space being reserved
 * @param startTime when the reservation begins
 * @param endTime when the reservation ends
 * @param groupSize the number of people attending
 */
public record ReservationRequest(
        @NotBlank(message = "bookerId is required")
        String bookerId,

        @NotNull(message = "spaceId is required")
        Long spaceId,

        @NotNull(message = "startTime is required")
        LocalDateTime startTime,

        @NotNull(message = "endTime is required")
        LocalDateTime endTime,

        @NotNull(message = "groupSize is required")
        @Positive(message = "groupSize must be greater than zero")
        Integer groupSize
) {
}
