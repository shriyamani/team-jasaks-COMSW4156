package com.jasaks.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ElementCollection;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;


import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Space: a bookable unit (eg. a room) within a place.
 */
@Entity
@Table(name = "spaces")
public class Space {
    /** Unique identifier of the space. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spaceId;

    /** The place this space belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    /** Name of the space. */
    private String spaceName;

    /** Maximum number of people the space can hold. */
    @Positive
    private int capacity;

    /** Price per hour to book the space. */
    @PositiveOrZero
    @Column(scale = 2)
    private BigDecimal hourlyRate;

    /** Amenities offered by the space. */
    @ElementCollection
    private Set<String> amenities = new HashSet<>();

    /** No-arg constructor required by JPA. */
    protected Space() { }

    /**
     * Creates a space.
     *
     * @param place the place this space belongs to
     * @param spaceName name of the space
     * @param capacity maximum number of people
     * @param hourlyRate price per hour
     * @param amenities amenities offered by the space
     */
    public Space(final Place place, final String spaceName,
                 final int capacity, final BigDecimal hourlyRate,
                 final Set<String> amenities) {
        this.place = place;
        this.spaceName = spaceName;
        this.capacity = capacity;
        this.hourlyRate = hourlyRate;
        this.amenities = amenities;
    }

    /**
     * Returns the space id.
     *
     * @return the space id
     */
    public Long getSpaceId() {
        return spaceId;
    }

    /**
     * Returns the place this space belongs to.
     *
     * @return the place
     */
    public Place getPlace() {
        return place;
    }

    /**
     * Returns the space name.
     *
     * @return the space name
     */
    public String getSpaceName() {
        return spaceName;
    }

    /**
     * Returns the capacity.
     *
     * @return the capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the hourly rate.
     *
     * @return the hourly rate
     */
    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    /**
     * Returns the amenities.
     *
     * @return the amenities
     */
    public Set<String> getAmenities() {
        return amenities;
    }

    /**
     * Sets the space name.
     *
     * @param spaceName the new space name
     */
    public void setSpaceName(final String spaceName) {
        this.spaceName = spaceName;
    }

    /**
     * Sets the capacity.
     *
     * @param capacity the new capacity
     */
    public void setCapacity(final int capacity) {
        this.capacity = capacity;
    }

    /**
     * Sets the hourly rate.
     *
     * @param hourlyRate the new hourly rate
     */
    public void setHourlyRate(final BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    /**
     * Replaces the amenities.
     *
     * @param amenities the new amenities
     */
    public void setAmenities(final Set<String> amenities) {
        this.amenities.clear();
        this.amenities.addAll(amenities);
    }
}
