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
    private String name;

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
     * @param name name of the place
     * @param timeZone time zone id of the place (eg. "America/New_York")
     * @param openTime daily opening time
     * @param closeTime daily closing time
     * @param refundHours hours before a reservation within which refunds
     *     apply
     * @param refundPercent percentage of the price that is refunded
     */
    public Place(final String name, final String timeZone,
                 final LocalTime openTime, final LocalTime closeTime,
                 final int refundHours, final int refundPercent) {
        this.name = name;
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
     * Returns the place name.
     *
     * @return the place name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the place time zone id.
     *
     * @return the time zone id
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
     * Sets the place name.
     *
     * @param name the new name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the daily opening time.
     *
     * @param openTime the new opening time
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
