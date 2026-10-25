package com.jasaks.model;

/**
 * Lifecycle status of a reservation.
 */
public enum ReservationStatus {
    /** The reservation is booked and upcoming. */
    CONFIRMED,
    /** The reservation's time period has passed. */
    COMPLETED,
    /** The reservation was cancelled. */
    CANCELLED
}
