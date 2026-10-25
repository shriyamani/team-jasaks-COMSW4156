package com.jasaks.dto;

import com.jasaks.model.Reservation;
import com.jasaks.model.ReservationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Representation of a reservation returned by the API.
 *
 * @param reservationId unique identifier of the reservation
 * @param bookerId identifier of the booker who created the reservation
 * @param spaceId identifier of the reserved space
 * @param spaceName name of the reserved space
 * @param createdAt timestamp when the reservation was created
 * @param startTime start time of the reservation
 * @param endTime end time of the reservation
 * @param groupSize size of the group attending
 * @param price total price charged for the reservation
 * @param status current status of the reservation
 */
public record ReservationResponse(
        Long reservationId,
        String bookerId,
        Long spaceId,
        String spaceName,
        Instant createdAt,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int groupSize,
        BigDecimal price,
        ReservationStatus status
) {

    /**
     * Creates a ReservationResponse from a Reservation entity.
     *
     * @param reservation the reservation entity
     * @return the reservation response DTO
     */
    public static ReservationResponse from(final Reservation reservation) {
        final Long spaceId = reservation.getSpace() != null
                ? reservation.getSpace().getSpaceId() : null;
        final String spaceName = reservation.getSpace() != null
                ? reservation.getSpace().getSpaceName() : null;
        final String bookerId = reservation.getBooker() != null
                ? reservation.getBooker().getBookerId() : null;

        return new ReservationResponse(
                reservation.getReservationId(),
                bookerId,
                spaceId,
                spaceName,
                reservation.getCreatedAt(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getGroupSize(),
                reservation.getPrice(),
                reservation.getStatus()
        );
    }
}

