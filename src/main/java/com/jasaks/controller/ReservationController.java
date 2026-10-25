package com.jasaks.controller;

import com.jasaks.dto.ReservationResponse;
import com.jasaks.service.ReservationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling reservation HTTP requests.
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    /**
     * Service handling reservation business logic.
     */
    private final ReservationService reservationService;

    /**
     * Constructs a ReservationController with the given service.
     *
     * @param reservationService the reservation service
     */
    public ReservationController(
            final ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Lists reservations for a user based on query parameters.
     *
     * @param userId the user ID
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param placeId optional place ID filter
     * @param spaceId optional space ID filter
     * @return list of matching reservations
     */
    @GetMapping
    public List<ReservationResponse> listUserReservations(
            @RequestParam final String userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime endDate,
            @RequestParam(required = false) final Long placeId,
            @RequestParam(required = false) final Long spaceId) {
        return reservationService.listUserReservations(
                userId, startDate, endDate, placeId, spaceId);
    }
}
