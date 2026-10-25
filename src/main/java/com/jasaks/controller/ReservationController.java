package com.jasaks.controller;

import com.jasaks.model.Reservation;
import com.jasaks.service.ReservationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling reservation HTTP requests.
 */
@RestController
public final class ReservationController {

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
     * @return HTTP response with matching reservations
     */
    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> listUserReservations(
            @RequestParam final String userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime endDate,
            @RequestParam(required = false) final Long placeId,
            @RequestParam(required = false) final Long spaceId) {
        List<Reservation> reservations =
                reservationService.listUserReservations(
                        userId, startDate, endDate, placeId, spaceId);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Lists reservations for a user using a path variable.
     *
     * @param userId the user ID from path
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @param placeId optional place ID filter
     * @param spaceId optional space ID filter
     * @return HTTP response with matching reservations
     */
    @GetMapping("/users/{userId}/reservations")
    public ResponseEntity<List<Reservation>> listUserReservationsByPath(
            @PathVariable final String userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            final LocalDateTime endDate,
            @RequestParam(required = false) final Long placeId,
            @RequestParam(required = false) final Long spaceId) {
        List<Reservation> reservations =
                reservationService.listUserReservations(
                        userId, startDate, endDate, placeId, spaceId);
        return ResponseEntity.ok(reservations);
    }

    /**
     * Exception handler for IllegalArgumentException.
     *
     * @param exception the caught exception
     * @return HTTP 400 Bad Request response with the error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(
            final IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
