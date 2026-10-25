package com.jasaks.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.jasaks.dto.SpaceMetrics;
import com.jasaks.dto.SpaceReportResponse;
import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
import com.jasaks.model.ReservationStatus;
import com.jasaks.model.Space;
import com.jasaks.repository.PlaceRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for ReportService core functionality.
 */
@ExtendWith(MockitoExtension.class)
public final class ReportServiceTest {

    /**
     * Mocked place repository.
     */
    @Mock
    private PlaceRepository placeRepository;

    /**
     * Mocked space repository.
     */
    @Mock
    private SpaceRepository spaceRepository;

    /**
     * Mocked reservation repository.
     */
    @Mock
    private ReservationRepository reservationRepository;

    /**
     * Service under test.
     */
    @InjectMocks
    private ReportService reportService;

    /**
     * Sample place.
     */
    private Place place;

    /**
     * Sample space one.
     */
    private Space spaceOne;

    /**
     * Sample space two.
     */
    private Space spaceTwo;

    /**
     * Sample reservation one.
     */
    private Reservation reservationOne;

    /**
     * Sample reservation two.
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
     * @throws Exception if reflection fails
     */
    @BeforeEach
    public void setUp() throws Exception {
        place = new Place("Butler Library", "America/New_York",
                LocalTime.of(8, 0), LocalTime.of(20, 0), 2, 50);
        setField(place, "placeId", 1L);

        spaceOne = new Space(place, "Room 209", 10,
                BigDecimal.valueOf(25.0), new HashSet<>());
        setField(spaceOne, "spaceId", 10L);

        spaceTwo = new Space(place, "Room 301", 20,
                BigDecimal.valueOf(50.0), new HashSet<>());
        setField(spaceTwo, "spaceId", 20L);

        final Booker booker = new Booker("jsw2219", "Janice Wong");

        reservationOne = new Reservation(booker, spaceOne,
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 12, 0),
                4, BigDecimal.valueOf(50.0));
        setField(reservationOne, "reservationId", 100L);

        reservationTwo = new Reservation(booker, spaceOne,
                LocalDateTime.of(2026, 11, 1, 14, 0),
                LocalDateTime.of(2026, 11, 1, 16, 0),
                6, BigDecimal.valueOf(50.0));
        setField(reservationTwo, "reservationId", 101L);
    }

    /**
     * Tests generating space report for a single space.
     */
    @Test
    public void testGenerateSpaceReportSingleSpace() {
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(spaceRepository.findById(10L)).thenReturn(Optional.of(spaceOne));
        when(reservationRepository.findBySpaceSpaceId(10L))
                .thenReturn(List.of(reservationOne, reservationTwo));

        final LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 11, 2, 0, 0);

        final SpaceReportResponse response = reportService.generateSpaceReport(
                1L, 10L, start, end);

        assertNotNull(response);
        assertEquals(1, response.spaces().size());

        final SpaceMetrics metrics = response.spaces().get(0);
        assertEquals(10L, metrics.spaceId());
        assertEquals(2, metrics.totalReservations());
        assertEquals(4.0, metrics.totalBookedHours());
        assertEquals(2.0, metrics.averageDurationHours());
        assertEquals(5.0, metrics.averageGroupSize());
        assertEquals(50.0, metrics.occupancyRate());
        assertEquals(new BigDecimal("100.0"), metrics.totalRevenue());
    }

    /**
     * Tests generating space report for all spaces in a place.
     */
    @Test
    public void testGenerateSpaceReportAllSpaces() {
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(spaceRepository.findByPlace_PlaceId(1L))
                .thenReturn(List.of(spaceOne, spaceTwo));
        when(reservationRepository.findBySpaceSpaceId(10L))
                .thenReturn(List.of(reservationOne));
        when(reservationRepository.findBySpaceSpaceId(20L))
                .thenReturn(Collections.emptyList());

        final LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 11, 2, 0, 0);

        final SpaceReportResponse response = reportService.generateSpaceReport(
                1L, null, start, end);

        assertNotNull(response);
        assertEquals(2, response.spaces().size());
        assertEquals(new BigDecimal("50.0"), response.totalRevenue());
    }

    /**
     * Tests report metrics when a space has zero reservations.
     */
    @Test
    public void testGenerateSpaceReportNoReservations() {
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(spaceRepository.findById(10L)).thenReturn(Optional.of(spaceOne));
        when(reservationRepository.findBySpaceSpaceId(10L))
                .thenReturn(Collections.emptyList());

        final LocalDateTime start = LocalDateTime.of(2026, 11, 1, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 11, 2, 0, 0);

        final SpaceReportResponse response = reportService.generateSpaceReport(
                1L, 10L, start, end);

        final SpaceMetrics metrics = response.spaces().get(0);
        assertEquals(0, metrics.totalReservations());
        assertEquals(0.0, metrics.totalBookedHours());
        assertEquals(0.0, metrics.averageDurationHours());
        assertEquals("N/A", metrics.peakBookingHour());
        assertEquals("N/A", metrics.peakBookingDate());
        assertEquals(BigDecimal.ZERO, metrics.totalRevenue());
    }

    /**
     * Tests validation error cases for input arguments.
     */
    @Test
    public void testGenerateSpaceReportValidationErrors() {
        final LocalDateTime start = LocalDateTime.of(2026, 12, 1, 0, 0);
        final LocalDateTime end = LocalDateTime.of(2026, 11, 1, 0, 0);

        assertThrows(ResponseStatusException.class, () ->
                reportService.generateSpaceReport(null, null, end, start));

        assertThrows(ResponseStatusException.class, () ->
                reportService.generateSpaceReport(1L, null, start, end));

        when(placeRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () ->
                reportService.generateSpaceReport(999L, null, end, start));
    }
}

