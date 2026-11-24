package com.jasaks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.jasaks.dto.SpaceMetrics;
import com.jasaks.dto.SpaceReportResponse;
import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
import com.jasaks.model.Space;
import com.jasaks.repository.PlaceRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link ReportService}.
 */
@ExtendWith(MockitoExtension.class)
public final class ReportServiceTest {

    /** Identifier of the fixture place. */
    private static final Long PLACE_ID = 1L;

    /** Identifier of the first fixture space. */
    private static final Long SPACE_ID = 10L;

    /** Identifier of the second fixture space. */
    private static final Long OTHER_SPACE_ID = 20L;

    /** Identifier used for missing entities. */
    private static final Long MISSING_ID = 999L;

    /** Start of the standard report window. */
    private static final LocalDateTime START =
            LocalDateTime.of(2026, 11, 1, 0, 0);

    /** End of the standard report window. */
    private static final LocalDateTime END = START.plusDays(1);

    /** Mocked place repository. */
    @Mock
    private PlaceRepository placeRepository;

    /** Mocked space repository. */
    @Mock
    private SpaceRepository spaceRepository;

    /** Mocked reservation repository. */
    @Mock
    private ReservationRepository reservationRepository;

    /** Service under test. */
    @InjectMocks
    private ReportService reportService;

    /** Fixture place. */
    private Place place;

    /** Fixture space. */
    private Space space;

    /** Second fixture space. */
    private Space otherSpace;

    /** First fixture reservation. */
    private Reservation first;

    /** Second fixture reservation. */
    private Reservation second;

    /**
     * Sets private ID fields via reflection.
     *
     * @param target the target object
     * @param fieldName the field name
     * @param value the value to set
     * @throws Exception if reflection fails
     */
    private void setField(final Object target, final String fieldName,
                          final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Creates a reservation fixture.
     *
     * @param target the space being reserved
     * @param start the start time
     * @param end the end time
     * @param group the group size
     * @param price the reservation price
     * @return the reservation
     * @throws Exception if the ID cannot be set
     */
    private Reservation reservation(final Space target,
                                    final LocalDateTime start,
                                    final LocalDateTime end,
                                    final int group,
                                    final BigDecimal price)
            throws Exception {
        final Booker booker = new Booker("jsw2219", "Janice Wong");
        final Reservation result = new Reservation(
                booker, target, start, end, group, price);
        setField(result, "reservationId", 100L);
        return result;
    }

    /**
     * Creates fresh fixtures for every test.
     *
     * @throws Exception if entity IDs cannot be set
     */
    @BeforeEach
    public void setUp() throws Exception {
        place = new Place("Butler Library", "America/New_York",
                LocalTime.of(8, 0), LocalTime.of(20, 0), 2, 50);
        setField(place, "placeId", PLACE_ID);

        space = new Space(place, "Study Room 101", 10,
                new BigDecimal("25.00"), new HashSet<>());
        setField(space, "spaceId", SPACE_ID);

        otherSpace = new Space(place, "Conference Room", 20,
                new BigDecimal("50.00"), new HashSet<>());
        setField(otherSpace, "spaceId", OTHER_SPACE_ID);

        first = reservation(space, START.plusHours(10),
                START.plusHours(12), 4, new BigDecimal("50.00"));
        second = reservation(space, START.plusHours(14),
                START.plusHours(16), 6, new BigDecimal("50.00"));
    }

    /**
     * Stubs the place lookup used by successful reports.
     */
    private void stubPlace() {
        when(placeRepository.findById(PLACE_ID))
                .thenReturn(Optional.of(place));
    }

    /**
     * Stubs the single-space lookup path.
     */
    private void stubSpace() {
        when(spaceRepository.findById(SPACE_ID))
                .thenReturn(Optional.of(space));
    }

    /**
     * Stubs reservations returned for the standard window.
     *
     * @param reservations the reservations to return
     */
    private void stubReservations(final List<Reservation> reservations) {
        when(reservationRepository.findActiveReservationsInWindow(
                SPACE_ID, START, END)).thenReturn(reservations);
    }

    /**
     * Input-validation partitions.
     */
    @Nested
    final class Validation {

        /**
         * A null place ID is rejected before any repository call.
         */
        @Test
        public void testNullPlaceIdIsRejected() {
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            null, null, START, END));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
            assertEquals("Place ID must be provided", error.getReason());
        }

        /**
         * A null start date is rejected.
         */
        @Test
        public void testNullStartDateIsRejected() {
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            PLACE_ID, null, null, END));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
            assertEquals("Start date and end date must be provided",
                    error.getReason());
        }

        /**
         * A null end date is rejected.
         */
        @Test
        public void testNullEndDateIsRejected() {
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            PLACE_ID, null, START, null));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }

        /**
         * Equal dates are the invalid boundary of the ordering rule.
         */
        @Test
        public void testEqualDatesAreRejected() {
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            PLACE_ID, null, START, START));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
            assertEquals("Start date must be before end date",
                    error.getReason());
        }

        /**
         * A reversed date range is invalid.
         */
        @Test
        public void testReversedDatesAreRejected() {
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            PLACE_ID, null, END, START));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }

        /**
         * A missing place is the invalid lookup partition.
         */
        @Test
        public void testMissingPlaceThrowsNotFound() {
            when(placeRepository.findById(MISSING_ID))
                    .thenReturn(Optional.empty());

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            MISSING_ID, null, START, END));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
            assertEquals("Place not found: " + MISSING_ID,
                    error.getReason());
        }

        /**
         * Zero and negative place IDs have no special guard and give 404.
         */
        @Test
        public void testZeroPlaceIdIsUnknownPartition() {
            when(placeRepository.findById(0L))
                    .thenReturn(Optional.empty());

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            0L, null, START, END));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        }

        /**
         * A missing space ID is the invalid space lookup partition.
         */
        @Test
        public void testMissingSpaceThrowsNotFound() {
            stubPlace();
            when(spaceRepository.findById(MISSING_ID))
                    .thenReturn(Optional.empty());

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            PLACE_ID, MISSING_ID, START, END));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        }

        /**
         * A space without a place is rejected.
         */
        @Test
        public void testSpaceWithNullPlaceIsRejected() throws Exception {
            stubPlace();
            final Space orphan = new Space(null, "Orphan", 4,
                    BigDecimal.ONE, new HashSet<>());
            setField(orphan, "spaceId", SPACE_ID);
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(orphan));

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            PLACE_ID, SPACE_ID, START, END));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
            assertEquals("Space does not belong to specified place",
                    error.getReason());
        }

        /**
         * A space belonging to another place is rejected.
         */
        @Test
        public void testSpaceFromOtherPlaceIsRejected() throws Exception {
            stubPlace();
            final Place other = new Place("Other", "America/New_York",
                    LocalTime.of(8, 0), LocalTime.of(20, 0), 2, 50);
            setField(other, "placeId", 2L);
            final Space foreign = new Space(other, "Foreign", 4,
                    BigDecimal.ONE, new HashSet<>());
            setField(foreign, "spaceId", SPACE_ID);
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(foreign));

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> reportService.generateSpaceReport(
                            PLACE_ID, SPACE_ID, START, END));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }

        /**
         * Ownership is by ID, so a different object with the same ID passes.
         */
        @Test
        public void testSamePlaceIdDifferentObjectIsAccepted()
                throws Exception {
            stubPlace();
            stubReservations(List.of());
            final Place sameId = new Place("Same", "America/New_York",
                    LocalTime.of(8, 0), LocalTime.of(20, 0), 2, 50);
            setField(sameId, "placeId", PLACE_ID);
            final Space local = new Space(sameId, "Local", 4,
                    BigDecimal.ONE, new HashSet<>());
            setField(local, "spaceId", SPACE_ID);
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(local));

            final SpaceReportResponse response =
                    reportService.generateSpaceReport(
                            PLACE_ID, SPACE_ID, START, END);

            assertEquals(1, response.spaces().size());
        }
    }

    /**
     * Metric calculation partitions for a single space.
     */
    @Nested
    final class SingleSpaceMetrics {

        /**
         * Two reservations exercise totals and the lowest-hour tie rule.
         */
        @Test
        public void testTwoReservationsProduceExpectedMetrics() {
            stubPlace();
            stubSpace();
            stubReservations(List.of(first, second));

            final SpaceReportResponse response =
                    reportService.generateSpaceReport(
                            PLACE_ID, SPACE_ID, START, END);
            final SpaceMetrics metrics = response.spaces().get(0);

            assertNotNull(response);
            assertEquals(SPACE_ID, metrics.spaceId());
            assertEquals("Study Room 101", metrics.spaceName());
            assertEquals(2, metrics.totalReservations());
            assertEquals(4.0, metrics.totalBookedHours());
            assertEquals(2.0, metrics.averageDurationHours());
            assertEquals(33.33, metrics.utilizationPercentage());
            assertEquals("10:00", metrics.peakBookingHour());
            assertEquals("2026-11-01", metrics.peakBookingDate());
            assertEquals(5.0, metrics.averageGroupSize());
            assertEquals(50.0, metrics.averageCapacityUsed());
            assertEquals(new BigDecimal("100.00"), metrics.totalRevenue());
            assertEquals("Butler Library", response.placeName());
            assertEquals(START, response.startDate());
            assertEquals(END, response.endDate());
        }

        /**
         * No reservations yields zeros and N/A peaks.
         */
        @Test
        public void testNoReservationsProducesZeroMetrics() {
            stubPlace();
            stubSpace();
            stubReservations(List.of());

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(0, metrics.totalReservations());
            assertEquals(0.0, metrics.totalBookedHours());
            assertEquals(0.0, metrics.averageDurationHours());
            assertEquals(0.0, metrics.utilizationPercentage());
            assertEquals("N/A", metrics.peakBookingHour());
            assertEquals("N/A", metrics.peakBookingDate());
            assertEquals(0.0, metrics.averageGroupSize());
            assertEquals(0.0, metrics.averageCapacityUsed());
            assertEquals(BigDecimal.ZERO, metrics.totalRevenue());
        }

        /**
         * Reservations wider than the window are clipped on both sides.
         */
        @Test
        public void testReservationsAreClippedToWindow()
                throws Exception {
            stubPlace();
            stubSpace();
            final Reservation clipped = reservation(space,
                    START.minusHours(2), END.plusHours(2), 4,
                    new BigDecimal("10.00"));
            stubReservations(List.of(clipped));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(24.0, metrics.totalBookedHours());
            assertEquals("00:00", metrics.peakBookingHour());
        }

        /**
         * Reservations touching window endpoints are not clipped.
         */
        @Test
        public void testWindowEndpointsAreInclusive() throws Exception {
            stubPlace();
            stubSpace();
            final Reservation touching = reservation(space,
                    START, END, 3, new BigDecimal("10.00"));
            stubReservations(List.of(touching));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(24.0, metrics.totalBookedHours());
            assertEquals("00:00", metrics.peakBookingHour());
        }

        /**
         * A sub-minute reservation contributes zero booked hours.
         */
        @Test
        public void testSubMinuteReservationCountsZeroHours()
                throws Exception {
            stubPlace();
            stubSpace();
            final Reservation shortBooking = reservation(space,
                    START.plusHours(10),
                    START.plusHours(10).plusSeconds(30), 1,
                    new BigDecimal("5.00"));
            stubReservations(List.of(shortBooking));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(0.0, metrics.totalBookedHours());
            assertEquals(1, metrics.totalReservations());
            assertEquals(new BigDecimal("5.00"), metrics.totalRevenue());
        }

        /**
         * A null price is skipped while a zero price is summed.
         */
        @Test
        public void testNullPriceIsSkippedAndZeroPriceIsSummed()
                throws Exception {
            stubPlace();
            stubSpace();
            final Reservation noPrice = reservation(space,
                    START.plusHours(8), START.plusHours(9), 2, null);
            final Reservation zeroPrice = reservation(space,
                    START.plusHours(9), START.plusHours(10), 2,
                    BigDecimal.ZERO);
            stubReservations(List.of(noPrice, zeroPrice));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(BigDecimal.ZERO, metrics.totalRevenue());
        }

        /**
         * Group size equal to capacity produces exactly 100 percent.
         */
        @Test
        public void testGroupSizeAtCapacityProducesFullUsage()
                throws Exception {
            stubPlace();
            stubSpace();
            final Reservation full = reservation(space,
                    START.plusHours(8), START.plusHours(9), 10,
                    BigDecimal.ONE);
            stubReservations(List.of(full));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(100.0, metrics.averageCapacityUsed());
        }

        /**
         * Group size above capacity is capped at 100 percent.
         */
        @Test
        public void testGroupSizeAboveCapacityIsCapped()
                throws Exception {
            stubPlace();
            stubSpace();
            final Reservation over = reservation(space,
                    START.plusHours(8), START.plusHours(9), 20,
                    BigDecimal.ONE);
            stubReservations(List.of(over));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(100.0, metrics.averageCapacityUsed());
        }

        /**
         * Zero capacity is the invalid partition that yields zero usage.
         */
        @Test
        public void testZeroCapacityProducesZeroUsage() throws Exception {
            stubPlace();
            final Space zeroCapacity = new Space(place, "Tiny", 0,
                    BigDecimal.ONE, new HashSet<>());
            setField(zeroCapacity, "spaceId", SPACE_ID);
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(zeroCapacity));
            final Reservation booked = reservation(zeroCapacity,
                    START.plusHours(8), START.plusHours(9), 2,
                    BigDecimal.ONE);
            when(reservationRepository.findActiveReservationsInWindow(
                    SPACE_ID, START, END)).thenReturn(List.of(booked));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(0.0, metrics.averageCapacityUsed());
        }

        /**
         * Utilization above 100 percent is capped.
         */
        @Test
        public void testUtilizationAboveFullIsCapped() throws Exception {
            stubPlace();
            final Space small = new Space(place, "Small", 1,
                    BigDecimal.ONE, new HashSet<>());
            setField(small, "spaceId", SPACE_ID);
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(small));
            final Reservation longBooking = reservation(small,
                    START.plusHours(8), END, 1, BigDecimal.ONE);
            when(reservationRepository.findActiveReservationsInWindow(
                    SPACE_ID, START, END))
                    .thenReturn(List.of(longBooking));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(100.0, metrics.utilizationPercentage());
        }

        /**
         * Dates with equal counts choose the lexicographically lowest.
         */
        @Test
        public void testDateTieChoosesLowestDate() throws Exception {
            stubPlace();
            stubSpace();
            final Reservation early = reservation(space,
                    START.plusDays(1).plusHours(10),
                    START.plusDays(1).plusHours(11), 2, BigDecimal.ONE);
            final Reservation late = reservation(space,
                    START.plusDays(2).plusHours(10),
                    START.plusDays(2).plusHours(11), 2, BigDecimal.ONE);
            final LocalDateTime wideEnd = START.plusDays(3);
            when(reservationRepository.findActiveReservationsInWindow(
                    SPACE_ID, START, wideEnd))
                    .thenReturn(List.of(early, late));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, wideEnd)
                    .spaces().get(0);

            assertEquals("2026-11-02", metrics.peakBookingDate());
        }

        /**
         * A sub-day window is treated as one full day.
         */
        @Test
        public void testSubDayWindowUsesOneDay() {
            stubPlace();
            stubSpace();
            final LocalDateTime shortEnd = START.plusMinutes(30);
            when(reservationRepository.findActiveReservationsInWindow(
                    SPACE_ID, START, shortEnd)).thenReturn(List.of());

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START,
                            shortEnd)
                    .spaces().get(0);

            assertEquals(0.0, metrics.utilizationPercentage());
        }

        /**
         * A two-day window doubles the available operating hours.
         */
        @Test
        public void testTwoDayWindowScalesAvailability() throws Exception {
            stubPlace();
            stubSpace();
            final Reservation booked = reservation(space,
                    START.plusHours(8), START.plusHours(12), 2,
                    BigDecimal.ONE);
            final LocalDateTime twoDays = START.plusDays(2);
            when(reservationRepository.findActiveReservationsInWindow(
                    SPACE_ID, START, twoDays))
                    .thenReturn(List.of(booked));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START,
                            twoDays)
                    .spaces().get(0);

            assertEquals(16.67, metrics.utilizationPercentage());
        }

        /**
         * Missing operating hours are treated as open all day.
         */
        @Test
        public void testNullOperatingHoursUseTwentyFourHours()
                throws Exception {
            place.setOpenTime(null);
            place.setCloseTime(null);
            stubPlace();
            stubSpace();
            final Reservation booked = reservation(space,
                    START.plusHours(8), START.plusHours(20), 2,
                    BigDecimal.ONE);
            stubReservations(List.of(booked));

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(50.0, metrics.utilizationPercentage());
        }

        /**
         * Equal operating hours are treated as open all day.
         */
        @Test
        public void testEqualOperatingHoursUseTwentyFourHours() {
            place.setOpenTime(LocalTime.of(8, 0));
            place.setCloseTime(LocalTime.of(8, 0));
            stubPlace();
            stubSpace();
            stubReservations(List.of());

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(0.0, metrics.utilizationPercentage());
        }

        /**
         * An overnight window still has twelve operating hours.
         */
        @Test
        public void testOvernightOperatingHours() {
            place.setOpenTime(LocalTime.of(20, 0));
            place.setCloseTime(LocalTime.of(8, 0));
            stubPlace();
            stubSpace();
            stubReservations(List.of());

            final SpaceMetrics metrics = reportService
                    .generateSpaceReport(PLACE_ID, SPACE_ID, START, END)
                    .spaces().get(0);

            assertEquals(0.0, metrics.utilizationPercentage());
        }

        /**
         * Null names are propagated into the response records.
         */
        @Test
        public void testNullNamesArePropagated() {
            place.setPlaceName(null);
            space.setSpaceName(null);
            stubPlace();
            stubSpace();
            stubReservations(List.of());

            final SpaceReportResponse response =
                    reportService.generateSpaceReport(
                            PLACE_ID, SPACE_ID, START, END);

            assertNull(response.placeName());
            assertNull(response.spaces().get(0).spaceName());
        }
    }

    /**
     * Partitions for reports that cover every space in a place.
     */
    @Nested
    final class AllSpaces {

        /**
         * A null space ID selects all spaces and sums their revenue.
         */
        @Test
        public void testNullSpaceIdEvaluatesEverySpace() {
            stubPlace();
            when(spaceRepository.findByPlacePlaceId(PLACE_ID))
                    .thenReturn(List.of(space, otherSpace));
            when(reservationRepository.findActiveReservationsInWindow(
                    SPACE_ID, START, END)).thenReturn(List.of(first));
            when(reservationRepository.findActiveReservationsInWindow(
                    OTHER_SPACE_ID, START, END)).thenReturn(List.of());

            final SpaceReportResponse response =
                    reportService.generateSpaceReport(
                            PLACE_ID, null, START, END);

            assertEquals(2, response.spaces().size());
            assertEquals(new BigDecimal("50.00"), response.totalRevenue());
        }

        /**
         * An empty space list is valid and produces an empty report.
         */
        @Test
        public void testNoSpacesProducesEmptyReport() {
            stubPlace();
            when(spaceRepository.findByPlacePlaceId(PLACE_ID))
                    .thenReturn(List.of());

            final SpaceReportResponse response =
                    reportService.generateSpaceReport(
                            PLACE_ID, null, START, END);

            assertEquals(0, response.spaces().size());
            assertEquals(BigDecimal.ZERO, response.totalRevenue());
        }
    }
}
