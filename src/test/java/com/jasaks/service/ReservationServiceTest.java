package com.jasaks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jasaks.dto.CancellationResponse;
import com.jasaks.dto.ReservationRequest;
import com.jasaks.dto.ReservationResponse;
import com.jasaks.dto.ReservationUpdateRequest;
import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
import com.jasaks.model.ReservationStatus;
import com.jasaks.model.Space;
import com.jasaks.repository.BookerRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link ReservationService}.
 */
@ExtendWith(MockitoExtension.class)
public final class ReservationServiceTest {

    /** Primary booker ID used by valid fixtures. */
    private static final String BOOKER_ID = "jsw2219";

    /** Booker ID used by ownership-failure fixtures. */
    private static final String OTHER_BOOKER_ID = "someoneelse";

    /** Time zone of all fixture places. */
    private static final ZoneId PLACE_ZONE =
            ZoneId.of("America/New_York");

    /** ID of the first fixture place. */
    private static final Long PLACE_ONE_ID = 1L;

    /** ID of the second fixture place. */
    private static final Long PLACE_TWO_ID = 2L;

    /** ID of the first fixture space. */
    private static final Long SPACE_ONE_ID = 10L;

    /** ID of the second fixture space. */
    private static final Long SPACE_TWO_ID = 20L;

    /** ID of the first fixture reservation. */
    private static final Long RESERVATION_ONE_ID = 100L;

    /** ID of the second fixture reservation. */
    private static final Long RESERVATION_TWO_ID = 200L;

    /** ID used to model missing entities. */
    private static final Long MISSING_ID = 999L;

    /** Mocked reservation repository. */
    @Mock
    private ReservationRepository reservationRepository;

    /** Mocked booker repository. */
    @Mock
    private BookerRepository bookerRepository;

    /** Mocked space repository. */
    @Mock
    private SpaceRepository spaceRepository;

    /** Service under test. */
    @InjectMocks
    private ReservationService reservationService;

    /** Primary booker. */
    private Booker booker;

    /** A different booker used for ownership failures. */
    private Booker otherBooker;

    /** Small space in the first place. */
    private Space spaceOne;

    /** Larger space in the second place. */
    private Space spaceTwo;

    /** Reservation in the first place. */
    private Reservation reservationOne;

    /** Reservation in the second place. */
    private Reservation reservationTwo;

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
     * Picks a future 10:00 start in the place time zone.
     *
     * @return a start time inside operating hours tomorrow
     */
    private LocalDateTime futureMorning() {
        final LocalDate tomorrow = LocalDate.now(PLACE_ZONE).plusDays(1);
        return LocalDateTime.of(tomorrow, LocalTime.of(10, 0));
    }

    /**
     * Builds a create request.
     *
     * @param bookerId the booker ID
     * @param spaceId the space ID
     * @param start the start time
     * @param end the end time
     * @param groupSize the group size
     * @return the request
     */
    private ReservationRequest createRequest(
            final String bookerId, final Long spaceId,
            final LocalDateTime start, final LocalDateTime end,
            final Integer groupSize) {
        return new ReservationRequest(
                bookerId, spaceId, start, end, groupSize);
    }

    /**
     * Builds an update request.
     *
     * @param bookerId the booker ID
     * @param spaceId the space ID, or null to keep it
     * @param start the start time, or null to keep it
     * @param end the end time, or null to keep it
     * @param groupSize the group size, or null to keep it
     * @return the request
     */
    private ReservationUpdateRequest updateRequest(
            final String bookerId, final Long spaceId,
            final LocalDateTime start, final LocalDateTime end,
            final Integer groupSize) {
        return new ReservationUpdateRequest(
                bookerId, spaceId, start, end, groupSize);
    }

    /**
     * Creates fresh fixtures for every test.
     *
     * @throws Exception if entity IDs cannot be set
     */
    @BeforeEach
    public void setUp() throws Exception {
        booker = new Booker(BOOKER_ID, "Janice Wong");
        otherBooker = new Booker(OTHER_BOOKER_ID, "Other Person");

        final Place placeOne = new Place("Butler Library",
                "America/New_York", LocalTime.of(8, 0),
                LocalTime.of(23, 0), 2, 50);
        setField(placeOne, "placeId", PLACE_ONE_ID);

        final Place placeTwo = new Place("Lerner Hall",
                "America/New_York", LocalTime.of(8, 0),
                LocalTime.of(23, 0), 2, 50);
        setField(placeTwo, "placeId", PLACE_TWO_ID);

        spaceOne = new Space(placeOne, "Room 209", 6,
                new BigDecimal("25.00"), new HashSet<>());
        setField(spaceOne, "spaceId", SPACE_ONE_ID);

        spaceTwo = new Space(placeTwo, "Party Space", 20,
                new BigDecimal("100.00"), new HashSet<>());
        setField(spaceTwo, "spaceId", SPACE_TWO_ID);

        reservationOne = new Reservation(booker, spaceOne,
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 12, 0), 4,
                new BigDecimal("50.00"));
        setField(reservationOne, "reservationId", RESERVATION_ONE_ID);

        reservationTwo = new Reservation(booker, spaceTwo,
                LocalDateTime.of(2026, 11, 15, 14, 0),
                LocalDateTime.of(2026, 11, 15, 16, 0), 8,
                new BigDecimal("200.00"));
        setField(reservationTwo, "reservationId", RESERVATION_TWO_ID);
    }

    /**
     * Asserts that an operation fails with the expected HTTP status.
     *
     * @param expected the expected status
     * @param operation the operation under test
     */
    private void assertStatus(final HttpStatus expected,
                              final Runnable operation) {
        final ResponseStatusException error = assertThrows(
                ResponseStatusException.class, operation::run);
        assertEquals(expected, error.getStatusCode());
    }

    /**
     * Compares a monetary value without depending on its scale.
     *
     * @param expected the expected amount
     * @param actual the actual amount
     */
    private void assertMoney(final String expected,
                             final BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    /**
     * Verifies that no reservation was saved.
     */
    private void verifyNoSave() {
        verify(reservationRepository, never()).save(any());
    }

    /**
     * Stubs save to echo the entity passed to it.
     */
    private void stubSave() {
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(call -> call.getArgument(0));
    }

    /**
     * Stubs the primary booker lookup.
     */
    private void stubBooker() {
        when(bookerRepository.findById(BOOKER_ID))
                .thenReturn(Optional.of(booker));
    }

    /**
     * Stubs the primary space lookup.
     */
    private void stubSpace() {
        when(spaceRepository.findById(SPACE_ONE_ID))
                .thenReturn(Optional.of(spaceOne));
    }

    /**
     * Prepares the first reservation with a future start and end.
     */
    private void prepareFutureReservation() {
        final LocalDateTime start = futureMorning();
        reservationOne.setStartTime(start);
        reservationOne.setEndTime(start.plusHours(2));
    }

    /**
     * Tests for listing reservations and applying filters.
     */
    @Nested
    final class ListOperations {

        /**
         * A valid booker ID with no filters returns all reservations.
         */
        @Test
        public void testListWithoutFilters() {
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of(reservationOne, reservationTwo));

            final List<ReservationResponse> results =
                    reservationService.listUserReservations(
                            BOOKER_ID, null, null, null, null);

            assertEquals(2, results.size());
            assertEquals(RESERVATION_ONE_ID,
                    results.get(0).reservationId());
            assertEquals(RESERVATION_TWO_ID,
                    results.get(1).reservationId());
        }

        /**
         * Null, empty, and whitespace-only IDs are invalid partitions.
         */
        @Test
        public void testInvalidBookerIdsAreRejected() {
            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.listUserReservations(
                            null, null, null, null, null));
            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.listUserReservations(
                            "", null, null, null, null));
            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.listUserReservations(
                            "  \t", null, null, null, null));
        }

        /**
         * Equal dates are the valid boundary of the ordering rule.
         */
        @Test
        public void testEqualDateFiltersAreAllowed() {
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of(reservationOne));
            final LocalDateTime boundary =
                    LocalDateTime.of(2026, 11, 1, 10, 0);

            final List<ReservationResponse> results =
                    reservationService.listUserReservations(
                            BOOKER_ID, boundary, boundary, null, null);

            assertEquals(0, results.size());
        }

        /**
         * A single null date filter leaves the other filter active.
         */
        @Test
        public void testSingleDateFilterIsAllowed() {
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of(reservationOne, reservationTwo));
            final LocalDateTime start =
                    LocalDateTime.of(2026, 11, 10, 0, 0);

            final List<ReservationResponse> results =
                    reservationService.listUserReservations(
                            BOOKER_ID, start, null, null, null);

            assertEquals(1, results.size());
            assertEquals(RESERVATION_TWO_ID,
                    results.get(0).reservationId());
        }

        /**
         * Reversed date filters are invalid.
         */
        @Test
        public void testReversedDateFiltersAreRejected() {
            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.listUserReservations(
                            BOOKER_ID, LocalDateTime.of(2026, 12, 1, 0, 0),
                            LocalDateTime.of(2026, 11, 1, 0, 0),
                            null, null));
        }

        /**
         * The start filter uses a strict greater-than comparison.
         */
        @Test
        public void testStartFilterBoundaries() {
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of(reservationOne));
            final LocalDateTime end = reservationOne.getEndTime();

            final List<ReservationResponse> atBoundary =
                    reservationService.listUserReservations(
                            BOOKER_ID, end, null, null, null);
            final List<ReservationResponse> belowBoundary =
                    reservationService.listUserReservations(
                            BOOKER_ID, end.minusMinutes(1), null,
                            null, null);

            assertTrue(atBoundary.isEmpty());
            assertEquals(1, belowBoundary.size());
        }

        /**
         * The end filter uses a strict less-than comparison.
         */
        @Test
        public void testEndFilterBoundaries() {
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of(reservationOne));
            final LocalDateTime start = reservationOne.getStartTime();

            final List<ReservationResponse> atBoundary =
                    reservationService.listUserReservations(
                            BOOKER_ID, null, start, null, null);
            final List<ReservationResponse> aboveBoundary =
                    reservationService.listUserReservations(
                            BOOKER_ID, null, start.plusMinutes(1),
                            null, null);

            assertTrue(atBoundary.isEmpty());
            assertEquals(1, aboveBoundary.size());
        }

        /**
         * Place and space filters accept only exact ID matches.
         */
        @Test
        public void testPlaceAndSpaceFilters() {
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of(reservationOne, reservationTwo));

            final List<ReservationResponse> matching =
                    reservationService.listUserReservations(
                            BOOKER_ID, null, null,
                            PLACE_ONE_ID, SPACE_ONE_ID);
            final List<ReservationResponse> wrongPlace =
                    reservationService.listUserReservations(
                            BOOKER_ID, null, null,
                            PLACE_TWO_ID, SPACE_ONE_ID);
            final List<ReservationResponse> unknown =
                    reservationService.listUserReservations(
                            BOOKER_ID, null, null,
                            MISSING_ID, MISSING_ID);

            assertEquals(1, matching.size());
            assertTrue(wrongPlace.isEmpty());
            assertTrue(unknown.isEmpty());
        }

        /**
         * Cancelled reservations are still returned by the list method.
         */
        @Test
        public void testCancelledReservationIsListed() {
            reservationOne.setStatus(ReservationStatus.CANCELLED);
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of(reservationOne));

            final List<ReservationResponse> results =
                    reservationService.listUserReservations(
                            BOOKER_ID, null, null, null, null);

            assertEquals(1, results.size());
            assertEquals(ReservationStatus.CANCELLED,
                    results.get(0).status());
        }

        /**
         * An empty repository result is a valid partition.
         */
        @Test
        public void testEmptyRepositoryResult() {
            when(reservationRepository.findByBookerBookerId(BOOKER_ID))
                    .thenReturn(List.of());

            assertTrue(reservationService.listUserReservations(
                    BOOKER_ID, null, null, null, null).isEmpty());
        }
    }

    /**
     * Tests for cancelling reservations and calculating refunds.
     */
    @Nested
    final class CancelOperations {

        /**
         * A confirmed future reservation is cancelled with a 50% refund.
         */
        @Test
        public void testCancelWithPartialRefund() {
            prepareFutureReservation();
            reservationOne.setStartTime(
                    LocalDateTime.now(PLACE_ZONE).plusHours(3));
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            final CancellationResponse response =
                    reservationService.cancelReservation(
                            RESERVATION_ONE_ID, BOOKER_ID);

            assertEquals(RESERVATION_ONE_ID, response.reservationId());
            assertEquals(ReservationStatus.CANCELLED, response.status());
            assertMoney("25.00", response.refundAmount());
            assertNotNull(response.cancelledAt());
            assertTrue(response.refundExplanation().contains("50%"));
            verify(reservationRepository).save(reservationOne);
        }

        /**
         * Cancelling below the refund lead time gives no refund.
         */
        @Test
        public void testCancelBelowRefundLeadTime() {
            reservationOne.setStartTime(
                    LocalDateTime.now(PLACE_ZONE).plusMinutes(90));
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            final CancellationResponse response =
                    reservationService.cancelReservation(
                            RESERVATION_ONE_ID, BOOKER_ID);

            assertMoney("0.00", response.refundAmount());
            assertTrue(response.refundExplanation()
                    .contains("less than 2hours"));
            verify(reservationRepository).save(reservationOne);
        }

        /**
         * The inclusive refund boundary is tested with a fixed clock.
         */
        @Test
        public void testCancelAtRefundLeadTimeBoundary() {
            final LocalDateTime fixedNow =
                    LocalDateTime.of(2030, 1, 1, 9, 0);
            reservationOne.setStartTime(fixedNow.plusHours(2));
            reservationOne.setEndTime(fixedNow.plusHours(3));
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            try (MockedStatic<LocalDateTime> mocked =
                         mockStatic(LocalDateTime.class,
                                 CALLS_REAL_METHODS)) {
                mocked.when(() -> LocalDateTime.now(PLACE_ZONE))
                        .thenReturn(fixedNow);

                final CancellationResponse response =
                        reservationService.cancelReservation(
                                RESERVATION_ONE_ID, BOOKER_ID);

                assertMoney("25.00", response.refundAmount());
            }
        }

        /**
         * A zero refund percent is the lower valid percentage boundary.
         */
        @Test
        public void testZeroRefundPercent() {
            prepareFutureReservation();
            spaceOne.getPlace().setRefundPercent(0);
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            final CancellationResponse response =
                    reservationService.cancelReservation(
                            RESERVATION_ONE_ID, BOOKER_ID);

            assertMoney("0.00", response.refundAmount());
        }

        /**
         * A full refund percent is the upper valid percentage boundary.
         */
        @Test
        public void testFullRefundPercent() {
            prepareFutureReservation();
            spaceOne.getPlace().setRefundPercent(100);
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            final CancellationResponse response =
                    reservationService.cancelReservation(
                            RESERVATION_ONE_ID, BOOKER_ID);

            assertMoney("50.00", response.refundAmount());
        }

        /**
         * A refund above the original price is rejected by the entity.
         */
        @Test
        public void testRefundAbovePriceIsRejected() {
            prepareFutureReservation();
            spaceOne.getPlace().setRefundPercent(150);
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            assertThrows(IllegalArgumentException.class,
                    () -> reservationService.cancelReservation(
                            RESERVATION_ONE_ID, BOOKER_ID));
            verifyNoSave();
        }

        /**
         * A missing reservation is the invalid lookup partition.
         */
        @Test
        public void testCancelMissingReservation() {
            when(reservationRepository.findById(MISSING_ID))
                    .thenReturn(Optional.empty());

            assertStatus(HttpStatus.NOT_FOUND,
                    () -> reservationService.cancelReservation(
                            MISSING_ID, BOOKER_ID));
        }

        /**
         * A different booker cannot cancel the reservation.
         */
        @Test
        public void testCancelByWrongBooker() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            assertStatus(HttpStatus.FORBIDDEN,
                    () -> reservationService.cancelReservation(
                            RESERVATION_ONE_ID, OTHER_BOOKER_ID));
            verifyNoSave();
        }

        /**
         * A non-confirmed reservation cannot be cancelled.
         */
        @Test
        public void testCancelNonConfirmedReservation() {
            prepareFutureReservation();
            reservationOne.setStatus(ReservationStatus.COMPLETED);
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            assertStatus(HttpStatus.CONFLICT,
                    () -> reservationService.cancelReservation(
                            RESERVATION_ONE_ID, BOOKER_ID));
            verifyNoSave();
        }

        /**
         * A reservation that has already started cannot be cancelled.
         */
        @Test
        public void testCancelPastReservation() {
            final LocalDateTime past = futureMorning().minusDays(2);
            reservationOne.setStartTime(past);
            reservationOne.setEndTime(past.plusHours(2));
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));

            assertStatus(HttpStatus.CONFLICT,
                    () -> reservationService.cancelReservation(
                            RESERVATION_ONE_ID, BOOKER_ID));
            verifyNoSave();
        }
    }

    /**
     * Tests for creating reservations.
     */
    @Nested
    final class CreateOperations {

        /**
         * A valid two-hour reservation is priced and saved.
         */
        @Test
        public void testCreateValidReservation() {
            final LocalDateTime start = futureMorning();
            final LocalDateTime end = start.plusHours(2);
            stubBooker();
            stubSpace();
            when(reservationRepository.hasOverlap(
                    SPACE_ONE_ID, start, end)).thenReturn(false);
            stubSave();

            final ReservationResponse response =
                    reservationService.createReservation(createRequest(
                            BOOKER_ID, SPACE_ONE_ID, start, end, 4));

            assertEquals(BOOKER_ID, response.bookerId());
            assertEquals(SPACE_ONE_ID, response.spaceId());
            assertEquals(start, response.startTime());
            assertEquals(4, response.groupSize());
            assertMoney("50.00", response.price());
            assertEquals(ReservationStatus.CONFIRMED, response.status());
            verify(reservationRepository).save(any(Reservation.class));
        }

        /**
         * A one-minute window is the smallest priced duration.
         */
        @Test
        public void testCreateOneMinuteReservation() {
            final LocalDateTime start = futureMorning();
            final LocalDateTime end = start.plusMinutes(1);
            stubBooker();
            stubSpace();
            when(reservationRepository.hasOverlap(
                    SPACE_ONE_ID, start, end)).thenReturn(false);
            stubSave();

            final ReservationResponse response =
                    reservationService.createReservation(createRequest(
                            BOOKER_ID, SPACE_ONE_ID, start, end, 1));

            assertMoney("0.42", response.price());
        }

        /**
         * A sub-minute duration rounds down to zero price.
         */
        @Test
        public void testCreateSubMinuteReservation() {
            final LocalDateTime start = futureMorning();
            final LocalDateTime end = start.plusSeconds(30);
            stubBooker();
            stubSpace();
            when(reservationRepository.hasOverlap(
                    SPACE_ONE_ID, start, end)).thenReturn(false);
            stubSave();

            final ReservationResponse response =
                    reservationService.createReservation(createRequest(
                            BOOKER_ID, SPACE_ONE_ID, start, end, 1));

            assertMoney("0.00", response.price());
        }

        /**
         * Equal and reversed windows are invalid boundaries.
         */
        @Test
        public void testCreateRejectsUnorderedWindow() {
            final LocalDateTime start = futureMorning();

            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    start, start, 1)));
            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    start, start.minusMinutes(1), 1)));
            verifyNoSave();
        }

        /**
         * A missing booker is the invalid lookup partition.
         */
        @Test
        public void testCreateRejectsMissingBooker() {
            final LocalDateTime start = futureMorning();
            when(bookerRepository.findById("unknown"))
                    .thenReturn(Optional.empty());

            assertStatus(HttpStatus.NOT_FOUND,
                    () -> reservationService.createReservation(
                            createRequest("unknown", SPACE_ONE_ID,
                                    start, start.plusHours(1), 1)));
            verifyNoSave();
        }

        /**
         * A missing space is the invalid lookup partition.
         */
        @Test
        public void testCreateRejectsMissingSpace() {
            final LocalDateTime start = futureMorning();
            stubBooker();
            when(spaceRepository.findById(MISSING_ID))
                    .thenReturn(Optional.empty());

            assertStatus(HttpStatus.NOT_FOUND,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, MISSING_ID,
                                    start, start.plusHours(1), 1)));
            verifyNoSave();
        }

        /**
         * A start time that is not strictly future is invalid.
         */
        @Test
        public void testCreateRejectsPastStart() {
            final LocalDateTime past = futureMorning().minusDays(2);
            stubBooker();
            stubSpace();

            assertStatus(HttpStatus.UNPROCESSABLE_CONTENT,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    past, past.plusHours(1), 1)));
            verifyNoSave();
        }

        /**
         * Group size equal to capacity is the upper valid boundary.
         */
        @Test
        public void testCreateAtCapacityBoundary() {
            final LocalDateTime start = futureMorning();
            final LocalDateTime end = start.plusHours(2);
            stubBooker();
            stubSpace();
            when(reservationRepository.hasOverlap(
                    SPACE_ONE_ID, start, end)).thenReturn(false);
            stubSave();

            final ReservationResponse response =
                    reservationService.createReservation(createRequest(
                            BOOKER_ID, SPACE_ONE_ID, start, end, 6));

            assertEquals(6, response.groupSize());
        }

        /**
         * Group size above capacity is invalid.
         */
        @Test
        public void testCreateAboveCapacityBoundary() {
            final LocalDateTime start = futureMorning();
            stubBooker();
            stubSpace();

            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    start, start.plusHours(2), 7)));
            verifyNoSave();
        }

        /**
         * Start before opening and end after closing are invalid.
         */
        @Test
        public void testCreateRejectsHoursOutsideOperatingWindow() {
            final LocalDateTime early = LocalDate.now(PLACE_ZONE)
                    .plusDays(1).atTime(7, 30);
            final LocalDateTime late = LocalDate.now(PLACE_ZONE)
                    .plusDays(1).atTime(23, 30);
            stubBooker();
            stubSpace();

            assertStatus(HttpStatus.UNPROCESSABLE_CONTENT,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    early, early.plusHours(2), 1)));
            assertStatus(HttpStatus.UNPROCESSABLE_CONTENT,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    late.minusHours(1), late, 1)));
            verifyNoSave();
        }

        /**
         * A window that crosses midnight is invalid.
         */
        @Test
        public void testCreateRejectsCrossDayWindow() {
            final LocalDateTime late = LocalDate.now(PLACE_ZONE)
                    .plusDays(1).atTime(22, 0);
            stubBooker();
            stubSpace();

            assertStatus(HttpStatus.UNPROCESSABLE_CONTENT,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    late, late.plusHours(2), 1)));
            verifyNoSave();
        }

        /**
         * An overlapping reservation blocks creation.
         */
        @Test
        public void testCreateRejectsOverlap() {
            final LocalDateTime start = futureMorning();
            final LocalDateTime end = start.plusHours(2);
            stubBooker();
            stubSpace();
            when(reservationRepository.hasOverlap(
                    SPACE_ONE_ID, start, end)).thenReturn(true);

            assertStatus(HttpStatus.CONFLICT,
                    () -> reservationService.createReservation(
                            createRequest(BOOKER_ID, SPACE_ONE_ID,
                                    start, end, 2)));
            verifyNoSave();
        }
    }

    /**
     * Tests for modifying reservations.
     */
    @Nested
    final class ModifyOperations {

        /**
         * Omitted fields keep their values and the price is recalculated.
         */
        @Test
        public void testModifyWithOnlyBookerId() {
            prepareFutureReservation();
            stubBooker();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            when(reservationRepository.hasOverlapExcluding(
                    SPACE_ONE_ID, reservationOne.getStartTime(),
                    reservationOne.getEndTime(), RESERVATION_ONE_ID))
                    .thenReturn(false);
            stubSave();

            final ReservationResponse response =
                    reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null, null, null,
                                    null));

            assertEquals(reservationOne.getStartTime(),
                    response.startTime());
            assertEquals(reservationOne.getEndTime(), response.endTime());
            assertEquals(4, response.groupSize());
            assertMoney("50.00", response.price());
        }

        /**
         * Changing time, group size, and space updates the reservation.
         */
        @Test
        public void testModifyAllProvidedFields() {
            prepareFutureReservation();
            final LocalDateTime newStart = futureMorning().plusDays(1);
            final LocalDateTime newEnd = newStart.plusHours(2);
            stubBooker();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            when(spaceRepository.findById(SPACE_TWO_ID))
                    .thenReturn(Optional.of(spaceTwo));
            when(reservationRepository.hasOverlapExcluding(
                    SPACE_TWO_ID, newStart, newEnd, RESERVATION_ONE_ID))
                    .thenReturn(false);
            stubSave();

            final ReservationResponse response =
                    reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, SPACE_TWO_ID,
                                    newStart, newEnd, 10));

            assertEquals(SPACE_TWO_ID, response.spaceId());
            assertEquals(10, response.groupSize());
            assertMoney("200.00", response.price());
        }

        /**
         * A missing reservation ID is invalid.
         */
        @Test
        public void testModifyMissingReservation() {
            when(reservationRepository.findById(MISSING_ID))
                    .thenReturn(Optional.empty());

            assertStatus(HttpStatus.NOT_FOUND,
                    () -> reservationService.modifyReservation(
                            MISSING_ID,
                            updateRequest(BOOKER_ID, null, null, null,
                                    null)));
            verifyNoSave();
        }

        /**
         * A missing booker ID is invalid.
         */
        @Test
        public void testModifyMissingBooker() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            when(bookerRepository.findById("unknown"))
                    .thenReturn(Optional.empty());

            assertStatus(HttpStatus.NOT_FOUND,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest("unknown", null, null, null,
                                    null)));
            verifyNoSave();
        }

        /**
         * A different booker cannot modify the reservation.
         */
        @Test
        public void testModifyByWrongBooker() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            when(bookerRepository.findById(OTHER_BOOKER_ID))
                    .thenReturn(Optional.of(otherBooker));

            assertStatus(HttpStatus.FORBIDDEN,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(OTHER_BOOKER_ID, null, null,
                                    null, null)));
            verifyNoSave();
        }

        /**
         * A cancelled reservation cannot be modified.
         */
        @Test
        public void testModifyCancelledReservation() {
            prepareFutureReservation();
            reservationOne.setStatus(ReservationStatus.CANCELLED);
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            stubBooker();

            assertStatus(HttpStatus.CONFLICT,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null, null, null,
                                    null)));
            verifyNoSave();
        }

        /**
         * A reservation whose original start has passed cannot be changed.
         */
        @Test
        public void testModifyStartedReservation() {
            final LocalDateTime past = futureMorning().minusDays(2);
            reservationOne.setStartTime(past);
            reservationOne.setEndTime(past.plusHours(2));
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            stubBooker();

            assertStatus(HttpStatus.CONFLICT,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null,
                                    futureMorning(), null, null)));
            verifyNoSave();
        }

        /**
         * A merged window whose end equals its start is invalid.
         */
        @Test
        public void testModifyRejectsEqualMergedTimes() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            stubBooker();
            final LocalDateTime start = reservationOne.getStartTime();

            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null, start, start,
                                    null)));
            verifyNoSave();
        }

        /**
         * A group above the target capacity is invalid.
         */
        @Test
        public void testModifyRejectsGroupAboveCapacity() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            stubBooker();

            assertStatus(HttpStatus.BAD_REQUEST,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null, null, null,
                                    7)));
            verifyNoSave();
        }

        /**
         * A merged start that is not future is invalid.
         */
        @Test
        public void testModifyRejectsPastMergedStart() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            stubBooker();

            assertStatus(HttpStatus.UNPROCESSABLE_CONTENT,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null,
                                    futureMorning().minusDays(2), null,
                                    null)));
            verifyNoSave();
        }

        /**
         * A merged window outside operating hours is invalid.
         */
        @Test
        public void testModifyRejectsHoursOutsideOperatingWindow() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            stubBooker();
            final LocalDateTime early = futureMorning().minusHours(3);

            assertStatus(HttpStatus.UNPROCESSABLE_CONTENT,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null, early,
                                    early.plusHours(2), null)));
            verifyNoSave();
        }

        /**
         * An overlapping reservation blocks modification.
         */
        @Test
        public void testModifyRejectsOverlap() {
            prepareFutureReservation();
            when(reservationRepository.findById(RESERVATION_ONE_ID))
                    .thenReturn(Optional.of(reservationOne));
            stubBooker();
            when(reservationRepository.hasOverlapExcluding(
                    SPACE_ONE_ID, reservationOne.getStartTime(),
                    reservationOne.getEndTime(), RESERVATION_ONE_ID))
                    .thenReturn(true);

            assertStatus(HttpStatus.CONFLICT,
                    () -> reservationService.modifyReservation(
                            RESERVATION_ONE_ID,
                            updateRequest(BOOKER_ID, null, null, null,
                                    null)));
            verifyNoSave();
            verify(reservationRepository).hasOverlapExcluding(
                    SPACE_ONE_ID, reservationOne.getStartTime(),
                    reservationOne.getEndTime(), RESERVATION_ONE_ID);
        }
    }
}
