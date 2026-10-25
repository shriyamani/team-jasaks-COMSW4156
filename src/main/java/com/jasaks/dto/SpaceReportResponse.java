package com.jasaks.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Report response containing space usage metrics.
 *
 * @param placeId the place ID
 * @param placeName the place name
 * @param startDate the start date of the reporting period
 * @param endDate the end date of the reporting period
 * @param spaces the list of space metrics
 * @param totalRevenue total revenue across all evaluated spaces
 */
public record SpaceReportResponse(
        Long placeId,
        String placeName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        List<SpaceMetrics> spaces,
        BigDecimal totalRevenue
) {
}

