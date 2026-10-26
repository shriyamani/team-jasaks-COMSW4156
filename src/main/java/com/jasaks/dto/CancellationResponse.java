package com.jasaks.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.jasaks.model.ReservationStatus;

public record CancellationResponse (
    Long reservationId,
    ReservationStatus status,
    BigDecimal refundAmount,
    Instant cancelledAt
) {}
