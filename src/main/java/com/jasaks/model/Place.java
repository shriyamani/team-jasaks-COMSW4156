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
@Table(name = "places")
public class Place {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    private String placeName;

    /**
     * The timezone that the Place you are creating is in (eg. "America/New_York")
     * When writing API endpoints, server should calculate "now()" time using this field
     * to avoid server (UTC) and place time zone mismatches. Make sure the server is
     * calculating now() in timeZone, not in UTC.
     *
     * LocalDateTime now = LocalDateTime.now(ZoneId.of(place.getTimeZone()));
     */
    @Column(nullable = false)
    private String timeZone;

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

    public Place(String placeName, String timeZone, LocalTime openTime, LocalTime closeTime, int refundHours,
                 int refundPercent) {
        this.placeName = placeName;
        this.timeZone = timeZone;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.refundHours = refundHours;
        this.refundPercent = refundPercent;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public String getTimeZone() {
        return timeZone;
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

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
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
