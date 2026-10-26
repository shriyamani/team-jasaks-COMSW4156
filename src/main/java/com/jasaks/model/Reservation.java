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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Positive
    private int groupSize;

    @PositiveOrZero
    @Column(scale = 2)
    private BigDecimal price;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id")
    private Space space;

    protected Reservation() {}

    public Reservation(Booker booker, Space space, LocalDateTime startTime, LocalDateTime endTime, int groupSize, BigDecimal price) {
        this.booker = booker;
        this.space = space;
        this.startTime = startTime;
        this.endTime = endTime;
        this.groupSize = groupSize;
        this.price = price;
        this.status = ReservationStatus.CONFIRMED;
        this.refundAmount = BigDecimal.ZERO;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Booker getBooker() {
        return booker;
    }

    public Space getSpace() {
        return space;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public BigDecimal getPrice() {
        return price;
    }

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

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public void setSpace(Space space) {
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
