package com.jasaks.service;

import com.jasaks.dto.ReservationResponse;
import com.jasaks.dto.CancellationResponse;
import com.jasaks.model.Reservation;
import com.jasaks.model.ReservationStatus;
import com.jasaks.repository.ReservationRepository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service class providing business logic for reservations.
 */
@Service
public class ReservationService {

    /**
     * Repository for reservation database access.
     */
    private final ReservationRepository reservationRepository;

    /**
     * Constructs a new ReservationService with the specified repository.
     *
     * @param reservationRepository the reservation repository
     */
    public ReservationService(
            final ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Retrieves reservations for a user with optional filtering criteria.
     *
     * @param userId the ID of the user
     * @param startDate the optional start date filter
     * @param endDate the optional end date filter
     * @param placeId the optional place ID filter
     * @param spaceId the optional space ID filter
     * @return list of matching reservation responses
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> listUserReservations(
            final String userId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final Long placeId,
            final Long spaceId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "User ID cannot be null or blank");
        }
        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date cannot be after end date");
        }

        final List<Reservation> reservations =
                reservationRepository.findByBookerBookerId(userId);

        return reservations.stream()
                .filter(reservation -> matchesFilter(
                        reservation, startDate, endDate, placeId, spaceId))
                .map(ReservationResponse::from)
                .toList();
    }

    /**
     * Checks if a reservation matches the specified filter criteria.
     *
     * @param reservation the reservation to evaluate
     * @param startDate the optional start date filter
     * @param endDate the optional end date filter
     * @param placeId the optional place ID filter
     * @param spaceId the optional space ID filter
     * @return true if the reservation matches all criteria, false otherwise
     */
    private boolean matchesFilter(
            final Reservation reservation,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final Long placeId,
            final Long spaceId) {
        if (startDate != null
                && !reservation.getEndTime().isAfter(startDate)) {
            return false;
        }
        if (endDate != null
                && !reservation.getStartTime().isBefore(endDate)) {
            return false;
        }
        if (placeId != null) {
            if (reservation.getSpace() == null
                    || reservation.getSpace().getPlace() == null
                    || !placeId.equals(
                            reservation.getSpace().getPlace().getPlaceId())) {
                return false;
            }
        }
        if (spaceId != null) {
            if (reservation.getSpace() == null
                    || !spaceId.equals(reservation.getSpace().getSpaceId())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cancels a reservation for a user.
     * 
     * @param reservationId the ID of the reservation to cancel
     * @param bookerId the ID of the user trying to cancel the reservation
     * @return the cancellation response
     */
    @Transactional
    public CancellationResponse cancelReservation(
            Long reservationId,
            String bookerId) {

        Reservation reservation = reservationRepository
            .findById(reservationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Reservation not found"
            ));

        if (!reservation.getBooker().getBookerId().equals(bookerId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You cannot cancel this reservation"
            );
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Reservation cannot be cancelled"
            );
        }

        if (!reservation.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Reservation has already started or passed"
            );
        }

        // full refund policy only for now
        BigDecimal refund = reservation.getPrice();

        reservation.cancel(refund);
        reservationRepository.save(reservation);

        return new CancellationResponse(
            reservation.getReservationId(),
            reservation.getStatus(),
            reservation.getRefundAmount(),
            reservation.getCancelledAt()
        );
    }
}
