package com.jasaks.service;

import com.jasaks.dto.ReservationResponse;
import com.jasaks.dto.CancellationResponse;
import com.jasaks.model.Reservation;
import com.jasaks.model.ReservationStatus;
import com.jasaks.model.Place;
import com.jasaks.repository.ReservationRepository;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
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
     * Calculates the refund amount for a reservation based on 
     * its cancellation policy and the time until the reservation starts.
     * 
     * @param reservation the reservation to calculate the refund for
     * @return the refund amount
     */
    private BigDecimal calculateRefund(final Reservation reservation) {
        Place place = reservation.getSpace().getPlace();
        final ZoneId placeZone = ZoneId.of(place.getTimeZone());
        final LocalDateTime currentTimeInPlace = LocalDateTime.now(placeZone);

        final Duration timeUntilStart = Duration.between(
            currentTimeInPlace,
            reservation.getStartTime()
        );

        int refundHours = place.getRefundHours();
        int refundPercent = place.getRefundPercent();

        if (timeUntilStart.compareTo(Duration.ofHours(refundHours)) >= 0) {
            final BigDecimal refundRate = BigDecimal.valueOf(refundPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

            return reservation.getPrice()
                .multiply(refundRate)
                .setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Generates an explanation for how refund amount was calculated 
     * based on the cancellation policy and timing.
     * 
     * @param reservation
     * @return the explanation string
     */
    private String getRefundExplanation(final Reservation reservation) {
        Place place = reservation.getSpace().getPlace();
        final ZoneId placeZone = ZoneId.of(place.getTimeZone());
        final LocalDateTime currentTimeInPlace = LocalDateTime.now(placeZone);
        
        final Duration timeUntilStart = java.time.Duration.between(
            currentTimeInPlace,
            reservation.getStartTime()
        );

        int refundHours = place.getRefundHours();
        int refundPercent = place.getRefundPercent();

        if (timeUntilStart.compareTo(Duration.ofHours(refundHours)) >= 0) {
            return "Refund = original price times " + refundPercent
                + "% because cancellation occurred at least "
                + refundHours + " hours before the start time.";
        }

        return "No refund because cancellation occurred less than "
            + refundHours + "hours before the start time.";
    
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
            final Long reservationId,
            final String bookerId) {

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

        if (!reservation.getStartTime().isAfter(LocalDateTime.now(ZoneId.of(
            reservation.getSpace().getPlace().getTimeZone())))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Reservation has already started or passed"
            );
        }

        BigDecimal refund = calculateRefund(reservation);

        reservation.cancel(refund);
        reservationRepository.save(reservation);

        return new CancellationResponse(
            reservation.getReservationId(),
            reservation.getStatus(),
            reservation.getRefundAmount(),
            reservation.getCancelledAt(),
            getRefundExplanation(reservation)
        );
    }
}
