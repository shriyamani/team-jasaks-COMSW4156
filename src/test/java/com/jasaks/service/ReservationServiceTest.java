package com.jasaks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
import com.jasaks.model.Space;
import com.jasaks.repository.ReservationRepository;
import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        final List<Reservation> results =
                reservationService.listUserReservations(
                        "jsw2219", null, null, null, null);

        assertNotNull(results);
        assertEquals(2, results.size());
    }

    /**
     * Tests filtering reservations by date range.
     */
    @Test
    public void testListReservationsFilterByDate() {
        when(reservationRepository.findByBookerBookerId("jsw2219"))
                .thenReturn(List.of(reservationOne, reservationTwo));

        final LocalDateTime start = LocalDateTime.of(2026, 11, 10, 0, 0);
        final List<Reservation> results =
                reservationService.listUserReservations(
                        "jsw2219", start, null, null, null);

        assertEquals(1, results.size());
        assertEquals(200L, results.get(0).getReservationId());
    }

    /**
     * Tests filtering reservations by place and space IDs.
     */
    @Test
    public void testListReservationsFilterBySpaceAndPlace() {
        when(reservationRepository.findByBookerBookerId("jsw2219"))
                .thenReturn(List.of(reservationOne, reservationTwo));

        final List<Reservation> results =
                reservationService.listUserReservations(
                        "jsw2219", null, null, 1L, 10L);

        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).getReservationId());
    }

    /**
     * Tests that invalid inputs throw IllegalArgumentException.
     */
    @Test
    public void testListReservationsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () ->
                reservationService.listUserReservations(
                        "", null, null, null, null));

        final LocalDateTime start = LocalDateTime.of(2026, 12, 1, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 11, 1, 0, 0);
        assertThrows(IllegalArgumentException.class, () ->
                reservationService.listUserReservations(
                        "jsw2219", start, end, null, null));
    }
}
