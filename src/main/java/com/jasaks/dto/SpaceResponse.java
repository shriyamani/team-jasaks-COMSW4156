package com.jasaks.dto;

import com.jasaks.model.Space;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a space returned by the API.
 *
 * @param spaceId identifier of the space
 * @param placeId identifier of the place containing the space
 * @param placeName name of the place containing the space
 * @param spaceName name of the space
 * @param capacity maximum number of people the space holds
 * @param hourlyRate hourly rental rate for the space
 * @param amenities amenities available in the space
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
    /**
     * Builds a response from a persisted space.
     *
     * @param space the space to convert
     * @return the response representation
     */
    public static SpaceResponse from(final Space space) {
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
