package com.jasaks.controller;

import com.jasaks.model.Booker;
import com.jasaks.model.Place;
import com.jasaks.model.Reservation;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SpaceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private BookerRepository bookerRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @BeforeEach
    void cleanDatabase() {
        reservationRepository.deleteAll();
        spaceRepository.deleteAll();
        bookerRepository.deleteAll();
        placeRepository.deleteAll();
    }

    @Test
    void createSpaceReturnsCreatedSpace() throws Exception {
        Place place = createPlace("Downtown");

        mockMvc.perform(post("/api/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Room A", 8, "25.50", place.getPlaceId(), "wifi", "whiteboard")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.spaceId").isNumber())
                .andExpect(jsonPath("$.placeId").value(place.getPlaceId()))
                .andExpect(jsonPath("$.placeName").value("Downtown"))
                .andExpect(jsonPath("$.spaceName").value("Room A"))
                .andExpect(jsonPath("$.capacity").value(8))
                .andExpect(jsonPath("$.hourlyRate").value(25.50))
                .andExpect(jsonPath("$.amenities", hasSize(2)));
    }

    @Test
    void listSpacesCanBeFilteredByPlace() throws Exception {
        Place downtown = createPlace("Downtown");
        Place uptown = createPlace("Uptown");
        createSpace(downtown, "Room A");
        createSpace(uptown, "Room B");

        mockMvc.perform(get("/api/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/spaces").param("placeId", downtown.getPlaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].spaceName").value("Room A"));
    }

    @Test
    void getSpaceReturnsSpace() throws Exception {
        Place place = createPlace("Downtown");
        Space space = createSpace(place, "Room A");

        mockMvc.perform(get("/api/spaces/{spaceId}", space.getSpaceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceId").value(space.getSpaceId()))
                .andExpect(jsonPath("$.spaceName").value("Room A"));
    }

    @Test
    void getSpaceReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/spaces/{spaceId}", 9999))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSpaceReplacesFields() throws Exception {
        Place place = createPlace("Downtown");
        Place newPlace = createPlace("Uptown");
        Space space = createSpace(place, "Room A");

        mockMvc.perform(put("/api/spaces/{spaceId}", space.getSpaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Room A - Renovated", 12, "40.00", newPlace.getPlaceId(), "projector")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaceName").value("Room A - Renovated"))
                .andExpect(jsonPath("$.capacity").value(12))
                .andExpect(jsonPath("$.hourlyRate").value(40.00))
                .andExpect(jsonPath("$.placeId").value(newPlace.getPlaceId()))
                .andExpect(jsonPath("$.placeName").value("Uptown"))
                .andExpect(jsonPath("$.amenities", hasSize(1)))
                .andExpect(jsonPath("$.amenities[0]").value("projector"));
    }

    @Test
    void updateSpaceReturnsNotFound() throws Exception {
        Place place = createPlace("Downtown");

        mockMvc.perform(put("/api/spaces/{spaceId}", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Room A", 4, "10.00", place.getPlaceId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSpaceRemovesIt() throws Exception {
        Place place = createPlace("Downtown");
        Space space = createSpace(place, "Room A");

        mockMvc.perform(delete("/api/spaces/{spaceId}", space.getSpaceId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/{spaceId}", space.getSpaceId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSpaceWithReservationsReturnsConflict() throws Exception {
        Place place = createPlace("Downtown");
        Space space = createSpace(place, "Room A");
        Booker booker = bookerRepository.save(new Booker("booker-1", "Ada"));
        reservationRepository.save(new Reservation(
                booker,
                space,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                2,
                new BigDecimal("10.00")
        ));

        mockMvc.perform(delete("/api/spaces/{spaceId}", space.getSpaceId()))
                .andExpect(status().isConflict());
    }

    @Test
    void createSpaceWithInvalidPayloadReturnsBadRequest() throws Exception {
        Place place = createPlace("Downtown");

        mockMvc.perform(post("/api/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("", 0, "-1.00", place.getPlaceId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSpaceWithUnknownPlaceReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("Room A", 4, "10.00", 9999L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchAvailableSpacesOkay() throws Exception {
        Place place = createPlace("Columbia University");
        Space space1 = createSpace(place, "Room A");
        Space space2 = spaceRepository.save(new Space(
                place, "Room B", 4, new BigDecimal("10.00"), Set.of("wifi", "tv")));

        LocalDateTime start = LocalDateTime.of(2026, 11, 25, 14, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 25, 18, 0, 0 );

        mockMvc.perform(get("/api/spaces/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spaceRequestJson(place.getPlaceId(), start, end, 6, "wifi")))
                .andDo(print())
                .andExpect(status().isOk());
    }


    private Place createPlace(String name) {
        return placeRepository.save(new Place(
                name,
                "America/New_York",
                LocalTime.of(8, 0),
                LocalTime.of(20, 0),
                24,
                50
        ));
    }

    private Space createSpace(Place place, String name) {
        return spaceRepository.save(new Space(
                place,
                name,
                6,
                new BigDecimal("20.00"),
                Set.of("wifi")
        ));
    }

    private String requestJson(String name, int capacity, String hourlyRate, Long placeId, String... amenities) {
        String amenitiesJson = amenities.length == 0
                ? ""
                : "\"" + String.join("\",\"", amenities) + "\"";

        return """
                {
                  "spaceName": "%s",
                  "capacity": %d,
                  "hourlyRate": %s,
                  "amenities": [%s],
                  "placeId": %d
                }
                """.formatted(name, capacity, hourlyRate, amenitiesJson, placeId);
    }

    private String spaceRequestJson(Long placeId, LocalDateTime start, LocalDateTime end, int minCapacity, String... amenities) {
        String amenitiesJson = amenities.length == 0
                ? ""
                : "\"" + String.join("\",\"", amenities) + "\"";

        return """
                {
                  "placeId": %d,
                  "startTime": "%s",
                  "endTime": "%s",
                  "minCapacity": %d,
                  "amenities": [%s]
                }
                """.formatted(placeId, start, end, minCapacity, amenitiesJson);
    }
}
