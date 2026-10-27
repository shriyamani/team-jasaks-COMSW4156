package com.jasaks.dto;

import java.math.BigDecimal;

/**
 * Representation of matching spaces from a search request returned by the API.
 *
 * @param request the object representing the space response with space
 *     information
 * @param estPrice the estimated price of the desired reservation time at
 *     the space
 */

public record SpaceSearchResponse(
        SpaceResponse request,
        BigDecimal estPrice
) {
}
