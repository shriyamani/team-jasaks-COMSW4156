package com.jasaks.service;

import com.jasaks.dto.SpaceMetrics;
import com.jasaks.dto.SpaceReportResponse;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
import com.jasaks.model.Space;
import com.jasaks.repository.PlaceRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service class for computing space usage metrics and reports.
 */
@Service
public class ReportService {

    /**
     * Logger for report service messages.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReportService.class);

    /**
     * Scale for rounding double metric values.
     */
    private static final int DECIMAL_SCALE = 2;

    /**
     * Total minutes in a standard day.
     */
    private static final double MINUTES_IN_DAY = 1440.0;

    /**
     * Total minutes in an hour.
     */
    private static final double MINUTES_IN_HOUR = 60.0;

    /**
     * Maximum percentage value.
     */
    private static final double MAX_PERCENTAGE = 100.0;

    /**
     * Repository for place database access.
     */
    private final PlaceRepository placeRepository;

    /**
     * Repository for space database access.
     */
    private final SpaceRepository spaceRepository;

    /**
     * Repository for reservation database access.
     */
    private final ReservationRepository reservationRepository;

    /**
     * Constructs a new ReportService with the required repositories.
     *
     * @param placeRepository the place repository
     * @param spaceRepository the space repository
     * @param reservationRepository the reservation repository
     */
    public ReportService(
            final PlaceRepository placeRepository,
            final SpaceRepository spaceRepository,
            final ReservationRepository reservationRepository) {
        this.placeRepository = placeRepository;
        this.spaceRepository = spaceRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Generates a usage report for spaces in a place over a date range.
     *
     * @param placeId the ID of the place
     * @param spaceId the optional space ID
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     * @return the space report response containing metrics
     */
    @Transactional(readOnly = true)
    public SpaceReportResponse generateSpaceReport(
            final Long placeId,
            final Long spaceId,
            final LocalDateTime startDate,
            final LocalDateTime endDate) {
        if (placeId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Place ID must be provided");
        }
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date and end date must be provided");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date cannot be after end date");
        }

        final Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Place not found: " + placeId));

        final List<Space> spacesToEvaluate;
        if (spaceId != null) {
            final Space space = spaceRepository.findById(spaceId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Space not found: " + spaceId));
            if (space.getPlace() == null
                    || !placeId.equals(space.getPlace().getPlaceId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Space does not belong to specified place");
            }
            spacesToEvaluate = List.of(space);
        } else {
            spacesToEvaluate = spaceRepository.findByPlace_PlaceId(placeId);
        }

        final List<SpaceMetrics> spaceMetricsList = new ArrayList<>();
        BigDecimal totalRevenueAcrossSpaces = BigDecimal.ZERO;

        for (final Space space : spacesToEvaluate) {
            final SpaceMetrics metrics = calculateMetricsForSpace(
                    space, place, startDate, endDate);
            spaceMetricsList.add(metrics);
            totalRevenueAcrossSpaces = totalRevenueAcrossSpaces.add(
                    metrics.totalRevenue());
        }

        return new SpaceReportResponse(
                placeId,
                place.getPlaceName(),
                startDate,
                endDate,
                spaceMetricsList,
                totalRevenueAcrossSpaces);
    }

    /**
     * Calculates metrics for an individual space over a date range.
     *
     * @param space the space
     * @param place the parent place
     * @param startDate the period start date
     * @param endDate the period end date
     * @return metrics for the space
     */
    private SpaceMetrics calculateMetricsForSpace(
            final Space space,
            final Place place,
            final LocalDateTime startDate,
            final LocalDateTime endDate) {
        final List<Reservation> activeReservations =
                reservationRepository.findActiveReservationsInWindow(
                        space.getSpaceId(), startDate, endDate);

        final int totalReservations = activeReservations.size();
        double totalBookedHours = 0.0;
        final Map<Integer, Integer> hourCounts = new HashMap<>();
        final Map<String, Integer> dateCounts = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalGroupSize = 0;

        for (final Reservation r : activeReservations) {
            final LocalDateTime effectiveStart =
                    r.getStartTime().isBefore(startDate)
                            ? startDate : r.getStartTime();
            final LocalDateTime effectiveEnd =
                    r.getEndTime().isAfter(endDate)
                            ? endDate : r.getEndTime();

            final long minutes = Duration.between(
                    effectiveStart, effectiveEnd).toMinutes();
            totalBookedHours += Math.max(0, minutes) / MINUTES_IN_HOUR;

            final int startHour = r.getStartTime().getHour();
            hourCounts.put(startHour,
                    hourCounts.getOrDefault(startHour, 0) + 1);

            final String bookingDate =
                    r.getStartTime().toLocalDate().toString();
            dateCounts.put(bookingDate,
                    dateCounts.getOrDefault(bookingDate, 0) + 1);

            if (r.getPrice() != null) {
                totalRevenue = totalRevenue.add(r.getPrice());
            }
            totalGroupSize += r.getGroupSize();
        }

        final double averageDurationHours = totalReservations > 0
                ? totalBookedHours / totalReservations : 0.0;

        final double dailyOperatingHours = calculateDailyOperatingHours(place);
        final long totalPeriodMinutes =
                Duration.between(startDate, endDate).toMinutes();
        final double periodDays = Math.max(
                1.0, totalPeriodMinutes / MINUTES_IN_DAY);
        final double totalAvailableHours = dailyOperatingHours * periodDays;

        final double rawUtilization = totalAvailableHours > 0.0
                ? (totalBookedHours / totalAvailableHours) * MAX_PERCENTAGE
                : 0.0;
        if (rawUtilization > MAX_PERCENTAGE) {
            LOGGER.warn("Space {} utilization exceeded 100%: {}%",
                    space.getSpaceId(), rawUtilization);
        }
        final double utilizationPercentage =
                Math.min(MAX_PERCENTAGE, rawUtilization);

        final String peakBookingHour = findPeakKey(hourCounts, true);
        final String peakBookingDate = findPeakKey(dateCounts, false);

        final double averageGroupSize = totalReservations > 0
                ? (double) totalGroupSize / totalReservations : 0.0;

        final double rawCapacityUsed = (space.getCapacity() > 0
                && totalReservations > 0)
                ? (averageGroupSize / space.getCapacity()) * MAX_PERCENTAGE
                : 0.0;
        if (rawCapacityUsed > MAX_PERCENTAGE) {
            LOGGER.warn("Space {} capacity usage exceeded 100%: {}%",
                    space.getSpaceId(), rawCapacityUsed);
        }
        final double averageCapacityUsed =
                Math.min(MAX_PERCENTAGE, rawCapacityUsed);

        return new SpaceMetrics(
                space.getSpaceId(),
                space.getSpaceName(),
                totalReservations,
                roundDouble(totalBookedHours),
                roundDouble(averageDurationHours),
                roundDouble(utilizationPercentage),
                peakBookingHour,
                peakBookingDate,
                roundDouble(averageGroupSize),
                roundDouble(averageCapacityUsed),
                totalRevenue);
    }

    /**
     * Calculates daily operating hours for a place.
     *
     * @param place the place
     * @return daily operating hours
     */
    private double calculateDailyOperatingHours(final Place place) {
        final LocalTime open = place.getOpenTime();
        final LocalTime close = place.getCloseTime();
        if (open == null || close == null) {
            return 24.0;
        }
        if (open.equals(close)) {
            return 24.0;
        }
        if (close.isAfter(open)) {
            return Duration.between(open, close).toMinutes() / MINUTES_IN_HOUR;
        }
        final long minutesBeforeMidnight =
                Duration.between(open, LocalTime.MAX).toMinutes() + 1;
        final long minutesAfterMidnight =
                Duration.between(LocalTime.MIN, close).toMinutes();
        return (minutesBeforeMidnight + minutesAfterMidnight)
                / MINUTES_IN_HOUR;
    }

    /**
     * Finds the peak key from a frequency map breaking ties with lowest key.
     *
     * @param <T> the comparable key type
     * @param countMap the frequency count map
     * @param isHour whether the key represents an hour
     * @return formatted peak key string or N/A
     */
    private <T extends Comparable<? super T>> String findPeakKey(
            final Map<T, Integer> countMap,
            final boolean isHour) {
        if (countMap == null || countMap.isEmpty()) {
            return "N/A";
        }
        T peakKey = null;
        int maxCount = -1;
        for (final Map.Entry<T, Integer> entry : countMap.entrySet()) {
            final int count = entry.getValue();
            final boolean isBetter = count > maxCount
                    || (count == maxCount && (peakKey == null
                    || entry.getKey().compareTo(peakKey) < 0));
            if (isBetter) {
                maxCount = count;
                peakKey = entry.getKey();
            }
        }
        if (peakKey == null) {
            return "N/A";
        }
        if (isHour && peakKey instanceof Integer hour) {
            return String.format("%02d:00", hour);
        }
        return peakKey.toString();
    }

    /**
     * Rounds a double to 2 decimal places.
     *
     * @param value the value to round
     * @return the rounded double
     */
    private double roundDouble(final double value) {
        return BigDecimal.valueOf(value)
                .setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
