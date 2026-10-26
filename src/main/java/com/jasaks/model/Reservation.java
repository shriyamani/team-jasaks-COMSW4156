package com.jasaks.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.persistence.PrePersist;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Reservation: A booker (client) books a space for a specific time period.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    /** Unique identifier of the reservation. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    /** Time the reservation was created. */
    @Column(nullable = false)
    private Instant createdAt;

    /** Start of the reserved time period. */
    @Column(nullable = false)
    private LocalDateTime startTime;

    /** End of the reserved time period. */
    @Column(nullable = false)
    private LocalDateTime endTime;

    /** Number of people in the group. */
    @Positive
    private int groupSize;

    /** Total price of the reservation. */
    @PositiveOrZero
    @Column(scale = 2)
    private BigDecimal price;

    /** Current status of the reservation. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    /** Time when the reservation was cancelled */
    @Column
    private Instant cancelledAt;

    /** The amount that will be refunded to the booker */
    @PositiveOrZero
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booker_id")
    private Booker booker;

    /** The space being reserved. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id")
    private Space space;

    /** No-arg constructor required by JPA. */
    protected Reservation() { }

    /**
     * Creates a confirmed reservation.
     *
     * @param booker the booker making the reservation
     * @param space the space being reserved
     * @param startTime start of the reserved time period
     * @param endTime end of the reserved time period
     * @param groupSize number of people in the group
     * @param price total price of the reservation
     */
    public Reservation(final Booker booker, final Space space,
                       final LocalDateTime startTime,
                       final LocalDateTime endTime,
                       final int groupSize, final BigDecimal price) {
        this.booker = booker;
        this.space = space;
        this.startTime = startTime;
        this.endTime = endTime;
        this.groupSize = groupSize;
        this.price = price;
        this.status = ReservationStatus.CONFIRMED;
        this.refundAmount = BigDecimal.ZERO;
    }

    /**
     * Returns the reservation id.
     *
     * @return the reservation id
     */
    public Long getReservationId() {
        return reservationId;
    }

    /**
     * Returns the creation time.
     *
     * @return the creation time
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the booker.
     *
     * @return the booker
     */
    public Booker getBooker() {
        return booker;
    }

    /**
     * Returns the reserved space.
     *
     * @return the space
     */
    public Space getSpace() {
        return space;
    }

    /**
     * Returns the start time.
     *
     * @return the start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time.
     *
     * @return the end time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * Returns the group size.
     *
     * @return the group size
     */
    public int getGroupSize() {
        return groupSize;
    }

    /**
     * Returns the price.
     *
     * @return the price
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Returns the status.
     *
     * @return the status
     */
    public ReservationStatus getStatus() {
        return status;
    }

    /**
     * Returns the time of cancellation if the reservation was cancelled.
     * 
     * @return the cancellation time
     */
    public Instant getCancelledAt() {
        return cancelledAt;
    }

    /**
     * Returns the refund amount if the reservation was cancelled.
     * 
     * @return the refund amount
     */
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Sets the start time.
     *
     * @param startTime the new start time
     */
    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Sets the price.
     *
     * @param price the new price
     */
    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    /**
     * Sets the group size.
     *
     * @param groupSize the new group size
     */
    public void setGroupSize(final int groupSize) {
        this.groupSize = groupSize;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     */
    public void setStatus(final ReservationStatus status) {
        this.status = status;
    }

    /**
     * Sets the reserved space.
     *
     * @param space the new space
     */
    public void setSpace(final Space space) {
        this.space = space;
    }

    /**
     * Cancels the reservation and sets the refund amount.
     * 
     * @param refundAmount the amount to be refunded
     */
    public void cancel(final BigDecimal refundAmount) {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Only confirmed reservations can be cancelled"
            );
        }

        if (refundAmount == null
                || refundAmount.signum() < 0
                || refundAmount.compareTo(price) > 0
                || refundAmount.scale() > 2) {
            throw new IllegalArgumentException(
                "Invalid refund amount"
            );
        }

        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.refundAmount = refundAmount;
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
