package com.jasaks.service;

import com.jasaks.model.Reservation;
import com.jasaks.repository.ReservationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class providing business logic for reservations.
 */
@Service
public final class ReservationService {

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
     * @return list of matching reservations
     */
    @Transactional(readOnly = true)
    public List<Reservation> listUserReservations(
            final String userId,
            final LocalDateTime startDate,
            final LocalDateTime endDate,
            final Long placeId,
            final Long spaceId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID cannot be null or blank");
        }
        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }

        List<Reservation> reservations =
                reservationRepository.findByBookerBookerId(userId);

        return reservations.stream()
                .filter(reservation -> matchesFilter(
                        reservation, startDate, endDate, placeId, spaceId))
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
                && reservation.getStartTime().isBefore(startDate)) {
            return false;
        }
        if (endDate != null
                && reservation.getStartTime().isAfter(endDate)) {
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
}
