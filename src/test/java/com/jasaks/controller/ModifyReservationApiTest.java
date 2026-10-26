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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the modify-reservation endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ModifyReservationApiTest {

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

    /** A date far enough ahead that the reservation has not passed. */
    private static final LocalDateTime DAY =
            LocalDateTime.of(2030, 6, 10, 0, 0);

    private Space space;
    private Booker booker;
    private Long reservationId;

    @BeforeEach
    void setUp() {
        Place place = placeRepository.save(new Place(
                "Butler Library", "America/New_York",
                LocalTime.of(9, 0), LocalTime.of(17, 0), 24, 50));
        space = spaceRepository.save(new Space(
                place, "Room 301", 8, new BigDecimal("30.00"), Set.of()));
        booker = bookerRepository.save(new Booker("booker-1", "Alice"));

        // 10:00-14:00, 4 hours at 30.00/hr = 120.00
        reservationId = reservationRepository.save(new Reservation(
                booker, space, DAY.withHour(10), DAY.withHour(14), 4,
                new BigDecimal("120.00"))).getReservationId();
    }

    private ResultActions send(final Long id, final String json)
            throws Exception {
        return mockMvc.perform(patch("/api/reservations/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void changesTheWindowAndRecalculatesThePrice() throws Exception {
        send(reservationId, """
            {
              "bookerId": "booker-1",
              "startTime": "2030-06-10T10:00:00",
              "endTime": "2030-06-10T12:00:00"
            }
            """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endTime")
                        .value("2030-06-10T12:00:00"))
                .andExpect(jsonPath("$.price").value(60.00))
                .andExpect(jsonPath("$.groupSize").value(4));
    }

    @Test
    void shrinkingItsOwnWindowDoesNotConflictWithItself() throws Exception {
        // 11:00-13:00 sits entirely inside the existing 10:00-14:00
        send(reservationId, """
            {
              "bookerId": "booker-1",
              "startTime": "2030-06-10T11:00:00",
              "endTime": "2030-06-10T13:00:00"
            }
            """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(60.00));
    }

    @Test
    void keepsFieldsThatAreLeftOut() throws Exception {
        send(reservationId, """
            {"bookerId": "booker-1", "groupSize": 2}
            """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupSize").value(2))
                .andExpect(jsonPath("$.startTime")
                        .value("2030-06-10T10:00:00"))
                .andExpect(jsonPath("$.endTime")
                        .value("2030-06-10T14:00:00"))
                .andExpect(jsonPath("$.price").value(120.00));
    }

    @Test
    void rejectsAGroupLargerThanTheSpace() throws Exception {
        send(reservationId, """
            {"bookerId": "booker-1", "groupSize": 20}
            """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAWindowOutsideOperatingHours() throws Exception {
        send(reservationId, """
            {
              "bookerId": "booker-1",
              "startTime": "2030-06-10T08:00:00",
              "endTime": "2030-06-10T10:00:00"
            }
            """)
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void rejectsOverlapWithAnotherReservation() throws Exception {
        reservationRepository.save(new Reservation(
                booker, space, DAY.withHour(15), DAY.withHour(16), 2,
                new BigDecimal("30.00")));

        send(reservationId, """
            {
              "bookerId": "booker-1",
              "startTime": "2030-06-10T14:00:00",
              "endTime": "2030-06-10T16:00:00"
            }
            """)
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsACancelledReservation() throws Exception {
        reservationRepository.findById(reservationId).orElseThrow()
                .setStatus(ReservationStatus.CANCELLED);
        reservationRepository.flush();

        send(reservationId, """
            {"bookerId": "booker-1", "groupSize": 2}
            """)
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAReservationThatHasAlreadyStarted() throws Exception {
        Long past = reservationRepository.save(new Reservation(
                booker, space,
                LocalDateTime.of(2020, 6, 10, 10, 0),
                LocalDateTime.of(2020, 6, 10, 12, 0), 2,
                new BigDecimal("60.00"))).getReservationId();

        send(past, """
            {"bookerId": "booker-1", "groupSize": 3}
            """)
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAnUnknownReservation() throws Exception {
        send(999999L, """
            {"bookerId": "booker-1", "groupSize": 2}
            """)
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAModificationByAnotherBooker() throws Exception {
        bookerRepository.save(new Booker("booker-2", "Bob"));

        send(reservationId, """
            {"bookerId": "booker-2", "groupSize": 2}
            """)
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsReschedulingIntoThePast() throws Exception {
        send(reservationId, """
            {
              "bookerId": "booker-1",
              "startTime": "2020-06-10T10:00:00",
              "endTime": "2020-06-10T12:00:00"
            }
            """)
                .andExpect(status().isUnprocessableContent());
    }
}
