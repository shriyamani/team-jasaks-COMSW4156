package com.jasaks.controller;

import com.jasaks.dto.SpaceReportResponse;
import com.jasaks.service.ReportService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for generating space usage reports.
 */
@RestController
@RequestMapping("/api/reports")
public final class ReportController {

    /**
     * Service handling report generation.
     */
    private final ReportService reportService;

    /**
     * Constructs a ReportController with the required service.
     *
     * @param reportService the report service
     */
    public ReportController(final ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Generates space usage report for a place and optional space.
     *
     * @param placeId the place ID
     * @param spaceId optional space ID
     * @param startDate period start date
     * @param endDate period end date
     * @return the space report response
     */
    @GetMapping("/spaces")
    public SpaceReportResponse getSpaceReport(
            @RequestParam final Long placeId,
            @RequestParam(required = false) final Long spaceId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime endDate) {
        return reportService.generateSpaceReport(
                placeId, spaceId, startDate, endDate);
    }
}

