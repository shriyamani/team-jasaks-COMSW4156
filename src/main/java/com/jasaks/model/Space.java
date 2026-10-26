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

@Entity
@Table(name = "spaces")
public class Space {
    /** Unique identifier of the space. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spaceId;

    /** Place that contains the space. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    /** Human-readable name of the space. */
    private String spaceName;

    /** Maximum number of people the space holds. */
    @Positive
    private int capacity;

    /** Hourly rental rate for the space. */
    @PositiveOrZero
    @Column(scale = 2)
    private BigDecimal hourlyRate;

    /** Amenities available in the space. */
    @ElementCollection
    private Set<String> amenities = new HashSet<>();

    protected Space() {
    }

    /**
     * Creates a space.
     *
     * @param place place that contains the space
     * @param spaceName name of the space
     * @param capacity maximum number of people the space holds
     * @param hourlyRate hourly rental rate for the space
     * @param amenities amenities available in the space
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
     * Returns the unique identifier of the space.
     *
     * @return the space identifier
     */
    public Long getSpaceId() {
        return spaceId;
    }

    /**
     * Returns the place that contains the space.
     *
     * @return the containing place
     */
    public Place getPlace() {
        return place;
    }

    /**
     * Sets the place that contains the space.
     *
     * @param place new containing place
     */
    public void setPlace(final Place place) {
        this.place = place;
    }

    /**
     * Returns the name of the space.
     *
     * @return the space name
     */
    public String getSpaceName() {
        return spaceName;
    }

    /**
     * Returns the capacity of the space.
     *
     * @return maximum number of people
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the hourly rental rate.
     *
     * @return the hourly rate
     */
    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    /**
     * Returns the amenities available in the space.
     *
     * @return the available amenities
     */
    public Set<String> getAmenities() {
        return amenities;
    }

    /**
     * Sets the name of the space.
     *
     * @param spaceName new space name
     */
    public void setSpaceName(final String spaceName) {
        this.spaceName = spaceName;
    }

    /**
     * Sets the capacity of the space.
     *
     * @param capacity new maximum number of people
     */
    public void setCapacity(final int capacity) {
        this.capacity = capacity;
    }

    /**
     * Sets the hourly rental rate.
     *
     * @param hourlyRate new hourly rate
     */
    public void setHourlyRate(final BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    /**
     * Replaces the amenities available in the space.
     *
     * @param amenities new amenities
     */
    public void setAmenities(final Set<String> amenities) {
        this.amenities.clear();
        this.amenities.addAll(amenities);
    }
}
