package com.jasaks.dto;

import com.jasaks.model.Space;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a space returned by the API.
 */
public record SpaceResponse(
        Long spaceId,
        Long placeId,
        String placeName,
        String spaceName,
        int capacity,
        BigDecimal hourlyRate,
        Set<String> amenities
) {
    public static SpaceResponse from(Space space) {
        return new SpaceResponse(
                space.getSpaceId(),
                space.getPlace().getPlaceId(),
                space.getPlace().getPlaceName(),
                space.getSpaceName(),
                space.getCapacity(),
                space.getHourlyRate(),
                new HashSet<>(space.getAmenities())
        );
    }
}
