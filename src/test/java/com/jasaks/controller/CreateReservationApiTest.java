package com.jasaks.controller;

import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Space;
import com.jasaks.repository.BookerRepository;
import com.jasaks.repository.PlaceRepository;
import com.jasaks.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the create-reservation endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CreateReservationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookerRepository bookerRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    private Long spaceId;

    @BeforeEach
    void setUp() {
        Place place = placeRepository.save(new Place(
                "Butler Library", "America/New_York",
                LocalTime.of(9, 0), LocalTime.of(17, 0), 24, 50));
        Space space = spaceRepository.save(new Space(
                place, "Room 301", 8, new BigDecimal("30.00"), Set.of()));
        spaceId = space.getSpaceId();
        bookerRepository.save(new Booker("booker-1", "Alice"));
    }

    private String body(final String start, final String end,
            final int groupSize) {
        return """
            {
              "bookerId": "booker-1",
              "spaceId": %d,
              "startTime": "%s",
              "endTime": "%s",
              "groupSize": %d
            }
            """.formatted(spaceId, start, end, groupSize);
    }

    private org.springframework.test.web.servlet.ResultActions send(
            final String json) throws Exception {
        return mockMvc.perform(post("/api/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void createsAConfirmedReservationWithAPrice() throws Exception {
        send(body("2030-06-10T10:00:00", "2030-06-10T12:00:00", 4))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").exists())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.price").value(60.00))
                .andExpect(jsonPath("$.bookerId").value("booker-1"));
    }

    @Test
    void rejectsAGroupLargerThanTheSpace() throws Exception {
        send(body("2030-06-10T10:00:00", "2030-06-10T12:00:00", 20))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAWindowOutsideOperatingHours() throws Exception {
        send(body("2030-06-10T08:00:00", "2030-06-10T10:00:00", 2))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void rejectsEndBeforeStart() throws Exception {
        send(body("2030-06-10T12:00:00", "2030-06-10T10:00:00", 2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnOverlappingReservation() throws Exception {
        send(body("2030-06-10T10:00:00", "2030-06-10T12:00:00", 2))
                .andExpect(status().isCreated());

        send(body("2030-06-10T11:00:00", "2030-06-10T13:00:00", 2))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAnUnknownSpace() throws Exception {
        send("""
            {
              "bookerId": "booker-1",
              "spaceId": 999999,
              "startTime": "2030-06-10T10:00:00",
              "endTime": "2030-06-10T12:00:00",
              "groupSize": 2
            }
            """).andExpect(status().isNotFound());
    }

    @Test
    void rejectsAWindowInThePast() throws Exception {
        send(body("2020-06-10T10:00:00", "2020-06-10T12:00:00", 2))
                .andExpect(status().isUnprocessableContent());
    }
}
