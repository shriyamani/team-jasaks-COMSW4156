package com.jasaks.service;

import com.jasaks.dto.ReservationRequest;
import com.jasaks.dto.ReservationResponse;
import com.jasaks.dto.CancellationResponse;
import com.jasaks.model.Booker;
import com.jasaks.model.Reservation;
import com.jasaks.model.ReservationStatus;
import com.jasaks.model.Place;
import com.jasaks.model.Space;
import com.jasaks.repository.BookerRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;


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

    /** Minutes in an hour, for converting a duration to billable hours. */
    private static final BigDecimal MINUTES_PER_HOUR =
            BigDecimal.valueOf(60);

    /** Decimal places retained while computing billable hours. */
    private static final int HOURS_SCALE = 4;

    /** Decimal places on a stored price. */
    private static final int PRICE_SCALE = 2;

    /**
     * Repository for reservation database access.
     */
    private final ReservationRepository reservationRepository;

    /** Repository for booker lookups. */
    private final BookerRepository bookerRepository;

    /** Repository for space lookups. */
    private final SpaceRepository spaceRepository;

    /**
     * Constructs a new ReservationService with the specified repositories.
     *
     * @param reservationRepository the reservation repository
     * @param bookerRepository the booker repository
     * @param spaceRepository the space repository
     */
    public ReservationService(
            final ReservationRepository reservationRepository,
            final BookerRepository bookerRepository,
            final SpaceRepository spaceRepository) {
        this.reservationRepository = reservationRepository;
        this.bookerRepository = bookerRepository;
        this.spaceRepository = spaceRepository;
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

    /**
     * Creates a reservation after validating the booker, space, capacity,
     * operating hours and overlaps.
     *
     * @param request the reservation to create
     * @return the confirmed reservation, including its price
     */
    @Transactional
    public ReservationResponse createReservation(
            final ReservationRequest request) {
        final LocalDateTime start = request.startTime();
        final LocalDateTime end = request.endTime();

        requireOrderedWindow(start, end);

        final Booker booker = findBooker(request.bookerId());
        final Space space = findSpace(request.spaceId());

        requireFutureStart(space.getPlace(), start);
        requireGroupFits(space, request.groupSize());
        requireWithinOperatingHours(space.getPlace(), start, end);
        requireNoOverlap(space, start, end, null);

        final Reservation reservation = new Reservation(
                booker, space, start, end, request.groupSize(),
                priceFor(space, start, end));

        return ReservationResponse.from(
                reservationRepository.save(reservation));
    }

    private void requireOrderedWindow(final LocalDateTime start,
            final LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw badRequest("endTime must be after startTime");
        }
    }

    private void requireFutureStart(final Place place,
            final LocalDateTime start) {
        final LocalDateTime now =
                LocalDateTime.now(ZoneId.of(place.getTimeZone()));
        if (!start.isAfter(now)) {
            throw unprocessable(
                    "startTime must be in the future");
        }
    }

    private void requireGroupFits(final Space space, final int groupSize) {
        if (groupSize > space.getCapacity()) {
            throw badRequest("groupSize " + groupSize
                    + " exceeds the capacity of space "
                    + space.getSpaceId() + " (" + space.getCapacity() + ")");
        }
    }

    private void requireWithinOperatingHours(final Place place,
            final LocalDateTime start, final LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())
                || start.toLocalTime().isBefore(place.getOpenTime())
                || end.toLocalTime().isAfter(place.getCloseTime())) {
            throw unprocessable("Reservation must fall on one day within "
                    + "the operating hours of place " + place.getPlaceId()
                    + " (" + place.getOpenTime() + " to "
                    + place.getCloseTime() + ")");
        }
    }

    // A null excludeId excludes nothing, as when creating.
    private void requireNoOverlap(final Space space,
            final LocalDateTime start, final LocalDateTime end,
            final Long excludeId) {
        final boolean clash = excludeId == null
                ? reservationRepository.hasOverlap(
                        space.getSpaceId(), start, end)
                : reservationRepository.hasOverlapExcluding(
                        space.getSpaceId(), start, end, excludeId);
        if (clash) {
            throw conflict("Space " + space.getSpaceId()
                    + " is already reserved for part of that period");
        }
    }

    private BigDecimal priceFor(final Space space,
            final LocalDateTime start, final LocalDateTime end) {
        final BigDecimal hours =
                BigDecimal.valueOf(Duration.between(start, end).toMinutes())
                        .divide(MINUTES_PER_HOUR, HOURS_SCALE,
                                RoundingMode.HALF_UP);
        return space.getHourlyRate()
                .multiply(hours)
                .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private Booker findBooker(final String bookerId) {
        return bookerRepository.findById(bookerId)
                .orElseThrow(() -> notFound(
                        "Booker not found: " + bookerId));
    }

    private Space findSpace(final Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> notFound(
                        "Space not found: " + spaceId));
    }

    private ResponseStatusException badRequest(final String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException unprocessable(final String message) {
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_CONTENT, message);
    }

    private ResponseStatusException notFound(final String message) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException conflict(final String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, message);
    }
}
