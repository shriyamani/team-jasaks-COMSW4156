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
    /** Unique identifier of the place. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long placeId;

    /** Human-readable name of the place. */
    private String placeName;

    /**
     * The time zone of the place (for example, "America/New_York").
     * Endpoints should calculate the current time in this zone, not UTC, to
     * avoid time zone mismatches.
     */
    @Column(nullable = false)
    private String timeZone;

    /** Time of day when the place opens. */
    @Column(nullable = false)
    private LocalTime openTime;

    /** Time of day when the place closes. */
    @Column(nullable = false)
    private LocalTime closeTime;

    /** Hours before a reservation start that qualify for a refund. */
    @PositiveOrZero
    private int refundHours;

    /** Percentage of the reservation price that is refunded. */
    @PositiveOrZero
    @Max(100)
    private int refundPercent;

    protected Place() {
    }

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
     * Returns the unique identifier of the place.
     *
     * @return the place identifier
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
     * Returns the time of day when the place opens.
     *
     * @return the opening time
     */
    public LocalTime getOpenTime() {
        return openTime;
    }

    /**
     * Returns the time of day when the place closes.
     *
     * @return the closing time
     */
    public LocalTime getCloseTime() {
        return closeTime;
    }

    /**
     * Returns the refund window in hours.
     *
     * @return hours before start that qualify for a refund
     */
    public int getRefundHours() {
        return refundHours;
    }

    /**
     * Returns the refund percentage.
     *
     * @return percentage of the price refunded
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
     * Sets the closing time of the place.
     *
     * @param closeTime new closing time
     */
    public void setCloseTime(final LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    /**
     * Sets the refund window in hours.
     *
     * @param refundHours new refund window
     */
    public void setRefundHours(final int refundHours) {
        this.refundHours = refundHours;
    }

    /**
     * Sets the refund percentage.
     *
     * @param refundPercent new refund percentage
     */
    public void setRefundPercent(final int refundPercent) {
        this.refundPercent = refundPercent;
    }
}
