package com.jasaks.controller;

import com.jasaks.dto.ReservationRequest;
import com.jasaks.dto.ReservationResponse;
import com.jasaks.dto.ReservationUpdateRequest;
import com.jasaks.dto.CancellationResponse;
import com.jasaks.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Cancels a reservation for a user.
     * 
     * @param id the reservation ID
     * @param bookerId the booker ID
     * @return the cancellation response containing refund details
     * @throws ResponseStatusException if the reservation cannot be cancelled
     * @throws IllegalArgumentException if the refund amount is invalid
     * @throws IllegalStateException if the reservation state or 
     * cancellation policy is invalid
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancellationResponse> cancelReservation(
            @PathVariable Long id,
            @RequestParam String bookerId) {

        CancellationResponse response =
            reservationService.cancelReservation(id, bookerId);

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a reservation.
     *
     * @param request the reservation to create
     * @return the confirmed reservation, including its price
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(
            @Valid @RequestBody final ReservationRequest request) {
        return reservationService.createReservation(request);
    }

    /**
     * Modifies a reservation; omitted fields keep their current value.
     *
     * @param reservationId the reservation to modify
     * @param request the fields to change
     * @return the updated reservation, including its new price
     */
    @PatchMapping("/{reservationId}")
    public ReservationResponse modifyReservation(
            @PathVariable final Long reservationId,
            @Valid @RequestBody final ReservationUpdateRequest request) {
        return reservationService.modifyReservation(reservationId, request);
    }
}
