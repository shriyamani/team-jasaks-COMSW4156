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

/**
 * Place: a location (eg. a building) that contains bookable spaces.
 */
@Entity
@Table(name = "places")
public class Place {
    /** Unique identifier of the place. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    /** Name of the place. */
    private String placeName;

    /**
     * The timezone that the Place you are creating is in
     * (eg. "America/New_York").
     * When writing API endpoints, server should calculate "now()" time
     * using this field to avoid server (UTC) and place time zone
     * mismatches. Make sure the server is calculating now() in timeZone,
     * not in UTC.
     *
     * LocalDateTime now = LocalDateTime.now(ZoneId.of(place.getTimeZone()));
     */
    @Column(nullable = false)
    private String timeZone;

    /** Daily opening time of the place. */
    @Column(nullable = false)
    private LocalTime openTime;

    /** Daily closing time of the place. */
    @Column(nullable = false)
    private LocalTime closeTime;

    /** Hours before a reservation within which refunds apply. */
    @PositiveOrZero
    private int refundHours;

    /** Percentage of the price that is refunded (0-100). */
    @PositiveOrZero
    @Max(100)
    private int refundPercent;

    /** No-arg constructor required by JPA. */
    protected Place() { }

    /**
     * Creates a place.
     *
     * @param placeName name of the place
     * @param timeZone IANA time zone identifier
     * @param openTime time the place opens
     * @param closeTime time the place closes
     * @param refundHours hours before start that qualify for a refund
     * @param refundPercent percentage of the price refunded
     */
    public Place(final String placeName, final String timeZone,
                 final LocalTime openTime, final LocalTime closeTime,
                 final int refundHours, final int refundPercent) {
        this.placeName = placeName;
        this.timeZone = timeZone;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.refundHours = refundHours;
        this.refundPercent = refundPercent;
    }

    /**
     * Returns the place id.
     *
     * @return the place id
     */
    public Long getPlaceId() {
        return placeId;
    }

    /**
     * Returns the name of the place.
     *
     * @return the place name
     */
    public String getPlaceName() {
        return placeName;
    }

    /**
     * Returns the time zone of the place.
     *
     * @return the IANA time zone identifier
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Returns the daily opening time.
     *
     * @return the opening time
     */
    public LocalTime getOpenTime() {
        return openTime;
    }

    /**
     * Returns the daily closing time.
     *
     * @return the closing time
     */
    public LocalTime getCloseTime() {
        return closeTime;
    }

    /**
     * Returns the refund window in hours.
     *
     * @return the refund hours
     */
    public int getRefundHours() {
        return refundHours;
    }

    /**
     * Returns the refund percentage.
     *
     * @return the refund percent
     */
    public int getRefundPercent() {
        return refundPercent;
    }

    /**
     * Sets the name of the place.
     *
     * @param placeName new name of the place
     */
    public void setPlaceName(final String placeName) {
        this.placeName = placeName;
    }

    /**
     * Sets the opening time of the place.
     *
     * @param openTime new opening time
     */
    public void setOpenTime(final LocalTime openTime) {
        this.openTime = openTime;
    }

    /**
     * Sets the daily closing time.
     *
     * @param closeTime the new closing time
     */
    public void setCloseTime(final LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    /**
     * Sets the refund window in hours.
     *
     * @param refundHours the new refund hours
     */
    public void setRefundHours(final int refundHours) {
        this.refundHours = refundHours;
    }

    /**
     * Sets the refund percentage.
     *
     * @param refundPercent the new refund percent
     */
    public void setRefundPercent(final int refundPercent) {
        this.refundPercent = refundPercent;
    }
}
