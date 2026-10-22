package com.jasaks.model;

import jakarta.persistence.Entity;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spaceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    private String spaceName;

    @Positive
    private int capacity;

    @PositiveOrZero
    @Column(scale=2)
    private BigDecimal hourlyRate;

    @ElementCollection
    private Set<String> amenities = new HashSet<>();

    protected Space() {}

    public Space(Place place, String spaceName, int capacity, BigDecimal hourlyRate, Set<String> amenities) {
        this.place = place;
        this.spaceName = spaceName;
        this.capacity = capacity;
        this.hourlyRate = hourlyRate;
        this.amenities = amenities;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public Place getPlace() {
        return place;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public Set<String> getAmenities() {
        return amenities;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public void setAmenities(Set<String> amenities) {
        this.amenities.clear();
        this.amenities.addAll(amenities);
    }
}
