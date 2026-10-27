package com.jasaks.controller;

import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
import com.jasaks.model.ReservationStatus;
import com.jasaks.model.Space;
import com.jasaks.repository.BookerRepository;
import com.jasaks.repository.PlaceRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the reservation listing and cancellation endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationControllerTests {

    /** Time zone used by the test place. */
    private static final ZoneId PLACE_ZONE = ZoneId.of("America/New_York");

    /** A date far enough ahead that the reservations have not passed. */
    private static final LocalDateTime DAY =
            LocalDateTime.of(2030, 6, 10, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BookerRepository bookerRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    private Place place;
    private Space space;
    private Booker booker;

    @BeforeEach
    void setUp() {
        place = placeRepository.save(new Place(
                "Butler Library", "America/New_York",
                LocalTime.of(9, 0), LocalTime.of(17, 0), 24, 50));
        space = spaceRepository.save(new Space(
                place, "Room 301", 8, new BigDecimal("30.00"), Set.of()));
        booker = bookerRepository.save(new Booker("booker-1", "Alice"));
        bookerRepository.save(new Booker("booker-2", "Bob"));
    }

    private Reservation saveReservation(
            final Booker owner, final Space bookedSpace,
            final LocalDateTime start, final LocalDateTime end,
            final BigDecimal price) {
        return reservationRepository.save(new Reservation(
                owner, bookedSpace, start, end, 2, price));
    }

    @Test
    void listsOnlyReservationsBelongingToTheRequestedUser() throws Exception {
        saveReservation(booker, space, DAY.withHour(10), DAY.withHour(12),
                new BigDecimal("60.00"));
        saveReservation(booker, space,
                DAY.plusDays(1).withHour(10), DAY.plusDays(1).withHour(12),
                new BigDecimal("60.00"));
        Booker otherBooker = bookerRepository.findById("booker-2")
                .orElseThrow();
        saveReservation(otherBooker, space,
                DAY.plusDays(2).withHour(10), DAY.plusDays(2).withHour(12),
                new BigDecimal("60.00"));

        mockMvc.perform(get("/api/reservations")
                        .param("userId", "booker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].bookerId",
                        everyItem(is("booker-1"))))
                .andExpect(jsonPath("$[0].spaceName").value("Room 301"));
    }

    @Test
    void filtersReservationsByDateRange() throws Exception {
        saveReservation(booker, space, DAY.withHour(10), DAY.withHour(12),
                new BigDecimal("60.00"));
        saveReservation(booker, space,
                DAY.plusDays(1).withHour(10), DAY.plusDays(1).withHour(12),
                new BigDecimal("60.00"));

        mockMvc.perform(get("/api/reservations")
                        .param("userId", "booker-1")
                        .param("startDate", "2030-06-11T00:00:00")
                        .param("endDate", "2030-06-12T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].startTime")
                        .value("2030-06-11T10:00:00"));
    }

    @Test
    void filtersReservationsBySpace() throws Exception {
        saveReservation(booker, space, DAY.withHour(10), DAY.withHour(12),
                new BigDecimal("60.00"));
        Space otherSpace = spaceRepository.save(new Space(
                place, "Room 302", 6, new BigDecimal("20.00"), Set.of()));
        Reservation chosen = saveReservation(booker, otherSpace,
                DAY.withHour(14), DAY.withHour(16),
                new BigDecimal("40.00"));

        mockMvc.perform(get("/api/reservations")
                        .param("userId", "booker-1")
                        .param("spaceId",
                                otherSpace.getSpaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reservationId")
                        .value(chosen.getReservationId()));
    }

    @Test
    void filtersReservationsByPlace() throws Exception {
        Place otherPlace = placeRepository.save(new Place(
                "Lerner Hall", "America/New_York",
                LocalTime.of(9, 0), LocalTime.of(17, 0), 24, 50));
        Space otherSpace = spaceRepository.save(new Space(
                otherPlace, "Room 401", 6, new BigDecimal("20.00"),
                Set.of()));
        saveReservation(booker, space, DAY.withHour(10), DAY.withHour(12),
                new BigDecimal("60.00"));
        Reservation chosen = saveReservation(booker, otherSpace,
                DAY.withHour(13), DAY.withHour(15),
                new BigDecimal("40.00"));

        mockMvc.perform(get("/api/reservations")
                        .param("userId", "booker-1")
                        .param("placeId",
                                otherPlace.getPlaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reservationId")
                        .value(chosen.getReservationId()));
    }

    @Test
    void returnsEmptyListForAUserWithoutReservations() throws Exception {
        saveReservation(booker, space, DAY.withHour(10), DAY.withHour(12),
                new BigDecimal("60.00"));

        mockMvc.perform(get("/api/reservations")
                        .param("userId", "unknown-booker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsARequestWithoutAUserId() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsABlankUserId() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .param("userId", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAStartDateAfterTheEndDate() throws Exception {
        mockMvc.perform(get("/api/reservations")
                        .param("userId", "booker-1")
                        .param("startDate", "2030-06-12T00:00:00")
                        .param("endDate", "2030-06-11T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelsAReservationAndReturnsTheRefund() throws Exception {
        LocalDateTime start = LocalDateTime.now(PLACE_ZONE).plusDays(3);
        Reservation reservation = saveReservation(
                booker, space, start, start.plusHours(2),
                new BigDecimal("60.00"));

        mockMvc.perform(post("/api/reservations/{id}/cancel",
                        reservation.getReservationId())
                        .param("bookerId", "booker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId")
                        .value(reservation.getReservationId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(30.00))
                .andExpect(jsonPath("$.cancelledAt").exists())
                .andExpect(jsonPath("$.refundExplanation")
                        .value(containsString("50%")));

        assertEquals(ReservationStatus.CANCELLED,
                reservationRepository.findById(reservation.getReservationId())
                        .orElseThrow().getStatus());
    }

    @Test
    void cancelsAReservationWithoutARefundInsideTheRefundWindow()
            throws Exception {
        LocalDateTime start = LocalDateTime.now(PLACE_ZONE).plusHours(1);
        Reservation reservation = saveReservation(
                booker, space, start, start.plusHours(2),
                new BigDecimal("60.00"));

        mockMvc.perform(post("/api/reservations/{id}/cancel",
                        reservation.getReservationId())
                        .param("bookerId", "booker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.refundExplanation")
                        .value(containsString("No refund")));
    }

    @Test
    void rejectsCancellationByAnotherBooker() throws Exception {
        LocalDateTime start = LocalDateTime.now(PLACE_ZONE).plusDays(3);
        Reservation reservation = saveReservation(
                booker, space, start, start.plusHours(2),
                new BigDecimal("60.00"));

        mockMvc.perform(post("/api/reservations/{id}/cancel",
                        reservation.getReservationId())
                        .param("bookerId", "booker-2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsCancellationOfAnUnknownReservation() throws Exception {
        mockMvc.perform(post("/api/reservations/{id}/cancel", 999999L)
                        .param("bookerId", "booker-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsCancellationOfAnAlreadyCancelledReservation()
            throws Exception {
        LocalDateTime start = LocalDateTime.now(PLACE_ZONE).plusDays(3);
        Reservation reservation = saveReservation(
                booker, space, start, start.plusHours(2),
                new BigDecimal("60.00"));
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.saveAndFlush(reservation);

        mockMvc.perform(post("/api/reservations/{id}/cancel",
                        reservation.getReservationId())
                        .param("bookerId", "booker-1"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsCancellationOfAReservationThatAlreadyStarted()
            throws Exception {
        LocalDateTime start = LocalDateTime.now(PLACE_ZONE).minusHours(2);
        Reservation reservation = saveReservation(
                booker, space, start, start.plusHours(1),
                new BigDecimal("30.00"));

        mockMvc.perform(post("/api/reservations/{id}/cancel",
                        reservation.getReservationId())
                        .param("bookerId", "booker-1"))
                .andExpect(status().isConflict());
    }
}
