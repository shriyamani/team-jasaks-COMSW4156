package com.jasaks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.jasaks.dto.ReservationResponse;
import com.jasaks.dto.CancellationResponse;
import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
import com.jasaks.model.Space;
import com.jasaks.model.CancellationPolicy;
import com.jasaks.model.ReservationStatus;
import com.jasaks.repository.ReservationRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for ReservationService core functionality.
 */
@ExtendWith(MockitoExtension.class)
public final class ReservationServiceTest {

    /**
     * Mocked reservation repository.
     */
    @Mock
    private ReservationRepository reservationRepository;

    /**
     * Service under test.
     */
    @InjectMocks
    private ReservationService reservationService;

    /**
     * First sample reservation.
     */
    private Reservation reservationOne;

    /**
     * Second sample reservation.
     */
    private Reservation reservationTwo;

    /**
     * Sets private ID fields via reflection.
     *
     * @param target the target object
     * @param fieldName the field name
     * @param value the value to set
     * @throws Exception if reflection fails
     */
    private void setField(
            final Object target,
            final String fieldName,
            final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Sets up test sample data.
     *
     * @throws Exception if setting fields fails
     */
    @BeforeEach
    public void setUp() throws Exception {
        final Booker booker = new Booker("jsw2219", "Janice Wong");

        final Place placeOne = new Place("Butler Library", "America/New_York",
                LocalTime.of(8, 0), LocalTime.of(23, 0), 2, 50);
        setField(placeOne, "placeId", 1L);

        final Place placeTwo = new Place("Lerner Hall", "America/New_York",
                LocalTime.of(8, 0), LocalTime.of(23, 0), 2, 50);
        setField(placeTwo, "placeId", 2L);

        final Space spaceOne = new Space(placeOne, "Room 209", 6,
                BigDecimal.valueOf(25.0), new HashSet<>());
        setField(spaceOne, "spaceId", 10L);

        final Space spaceTwo = new Space(placeTwo, "Party Space", 20,
                BigDecimal.valueOf(100.0), new HashSet<>());
        setField(spaceTwo, "spaceId", 20L);

        reservationOne = new Reservation(booker, spaceOne,
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 12, 0),
                4, BigDecimal.valueOf(50.0));
        setField(reservationOne, "reservationId", 100L);

        reservationTwo = new Reservation(booker, spaceTwo,
                LocalDateTime.of(2026, 11, 15, 14, 0),
                LocalDateTime.of(2026, 11, 15, 16, 0),
                8, BigDecimal.valueOf(200.0));
        setField(reservationTwo, "reservationId", 200L);
    }

    /**
     * Tests listing user reservations without any filters.
     */
    @Test
    public void testListReservationsSuccess() {
        when(reservationRepository.findByBookerBookerId("jsw2219"))
                .thenReturn(List.of(reservationOne, reservationTwo));

        final List<ReservationResponse> results =
                reservationService.listUserReservations(
                        "jsw2219", null, null, null, null);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(100L, results.get(0).reservationId());
        assertEquals(200L, results.get(1).reservationId());
    }

    /**
     * Tests filtering reservations by date range with overlap logic.
     */
    @Test
    public void testListReservationsFilterByDate() {
        when(reservationRepository.findByBookerBookerId("jsw2219"))
                .thenReturn(List.of(reservationOne, reservationTwo));

        final LocalDateTime start = LocalDateTime.of(2026, 11, 10, 0, 0);
        final List<ReservationResponse> results =
                reservationService.listUserReservations(
                        "jsw2219", start, null, null, null);

        assertEquals(1, results.size());
        assertEquals(200L, results.get(0).reservationId());

        final LocalDateTime overlapStart =
                LocalDateTime.of(2026, 11, 1, 11, 0);
        final LocalDateTime overlapEnd =
                LocalDateTime.of(2026, 11, 1, 13, 0);
        final List<ReservationResponse> overlapResults =
                reservationService.listUserReservations(
                        "jsw2219", overlapStart, overlapEnd, null, null);

        assertEquals(1, overlapResults.size());
        assertEquals(100L, overlapResults.get(0).reservationId());
    }

    /**
     * Tests filtering reservations by place and space IDs.
     */
    @Test
    public void testListReservationsFilterBySpaceAndPlace() {
        when(reservationRepository.findByBookerBookerId("jsw2219"))
                .thenReturn(List.of(reservationOne, reservationTwo));

        final List<ReservationResponse> results =
                reservationService.listUserReservations(
                        "jsw2219", null, null, 1L, 10L);

        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).reservationId());
    }

    /**
     * Tests that invalid inputs throw ResponseStatusException.
     */
    @Test
    public void testListReservationsInvalidInput() {
        final ResponseStatusException blankIdException =
                assertThrows(ResponseStatusException.class, () ->
                        reservationService.listUserReservations(
                                "", null, null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                blankIdException.getStatusCode());

        final LocalDateTime start = LocalDateTime.of(2026, 12, 1, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 11, 1, 0, 0);
        final ResponseStatusException dateOrderException =
                assertThrows(ResponseStatusException.class, () ->
                        reservationService.listUserReservations(
                                "jsw2219", start, end, null, null));
        assertEquals(HttpStatus.BAD_REQUEST,
                dateOrderException.getStatusCode());
    }

    /**
     * Tests successful cancellation with a full refund.
     */
    @Test
    public void testCancelReservationSuccess() {
        reservationOne.getSpace().setCancellationPolicy(
                CancellationPolicy.FLEXIBLE);

        reservationOne.setStartTime(
                LocalDateTime.now().plusHours(48));

        when(reservationRepository.findById(100L))
                .thenReturn(Optional.of(reservationOne));

        final CancellationResponse response =
                reservationService.cancelReservation(100L, "jsw2219");

        assertNotNull(response);
        assertEquals(100L, response.reservationId());
        assertEquals(ReservationStatus.CANCELLED, response.status());
        assertEquals(0, new BigDecimal("50.00").compareTo(
                response.refundAmount()));
        assertNotNull(response.cancelledAt());

        verify(reservationRepository).save(reservationOne);
    }

    /**
     * Tests a partial refund under the flexible policy.
     */
    @Test
    public void testCancelReservationPartialRefund() {
        reservationOne.getSpace().setCancellationPolicy(
                CancellationPolicy.FLEXIBLE);

        reservationOne.setStartTime(
                LocalDateTime.now().plusHours(12));

        when(reservationRepository.findById(100L))
                .thenReturn(Optional.of(reservationOne));

        final CancellationResponse response =
                reservationService.cancelReservation(100L, "jsw2219");

        assertEquals(ReservationStatus.CANCELLED, response.status());
        assertEquals(0, new BigDecimal("25.00").compareTo(
                response.refundAmount()));
    }

    /**
     * Tests a full refund under the flexible policy.
     */
    @Test
    public void testFlexiblePolicyFullRefund() {
        testFullRefund(CancellationPolicy.FLEXIBLE, 25);
    }

    /**
     * Tests a full refund under the moderate policy.
     */
    @Test
    public void testModeratePolicyFullRefund() {
        testFullRefund(CancellationPolicy.MODERATE, 169);
    }

    /**
     * Tests a full refund under the strict policy.
     */
    @Test
    public void testStrictPolicyFullRefund() {
        testFullRefund(CancellationPolicy.STRICT, 337);
    }

    /**
     * Verifies a full refund for the specified cancellation policy.
     * 
     * @param policy the cancellation policy
     * @param hoursUntilStart hours remaining before the reservation
     */
    private void testFullRefund(
        final CancellationPolicy policy,
        final long hoursUntilStart) {

        reservationOne.getSpace().setCancellationPolicy(policy);
        reservationOne.setStartTime(
                LocalDateTime.now().plusHours(hoursUntilStart));

        when(reservationRepository.findById(100L))
                .thenReturn(Optional.of(reservationOne));

        final CancellationResponse response =
                reservationService.cancelReservation(100L, "jsw2219");

        assertEquals(ReservationStatus.CANCELLED, response.status());
        assertEquals(0, new BigDecimal("50.00").compareTo(
                response.refundAmount()));
  }
}
