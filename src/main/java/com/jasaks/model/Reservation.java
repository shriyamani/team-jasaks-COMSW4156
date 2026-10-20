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

    @PrePersist
    public void onCreate() {
        this.createdAt = Instant.now();
    }
}
