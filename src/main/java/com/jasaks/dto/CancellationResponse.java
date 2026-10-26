package com.jasaks.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.jasaks.model.ReservationStatus;

/**
 * Represents the confirmation returned after cancelling a reservation.
 *
 * @param reservationId the ID of the cancelled reservation
 * @param status the reservation's status after cancellation
 * @param refundAmount the amount to be refunded
 * @param cancelledAt the time the reservation was cancelled
 * @param refundExplanation explanation of how the refund was calculated
 */
public record CancellationResponse(
    Long reservationId,
    ReservationStatus status,
    BigDecimal refundAmount,
    Instant cancelledAt,
    String refundExplanation
) {
}
