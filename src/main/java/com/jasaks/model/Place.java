package com.jasaks.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalTime;

@Entity
@Table(name="places")
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    private String name;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @PositiveOrZero
    private int refundHours;

    @PositiveOrZero
    @Max(100)
    private int refundPercent;

    protected Place() {}

    public Place(String name, LocalTime openTime, LocalTime closeTime, int refundHours, int refundPercent) {
        this.name = name;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.refundHours = refundHours;
        this.refundPercent = refundPercent;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public String getName() {
        return name;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public int getRefundHours() {
        return refundHours;
    }

    public int getRefundPercent() {
        return refundPercent;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    public void setRefundHours(int refundHours) {
        this.refundHours = refundHours;
    }

    public void setRefundPercent(int refundPercent) {
        this.refundPercent = refundPercent;
    }
}
