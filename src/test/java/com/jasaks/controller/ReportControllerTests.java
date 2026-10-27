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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the space usage report endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerTests {

    /** Start of the reporting period. */
    private static final LocalDateTime START =
            LocalDateTime.of(2026, 11, 1, 0, 0);

    /** End of the reporting period. */
    private static final LocalDateTime END =
            LocalDateTime.of(2026, 11, 3, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private BookerRepository bookerRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Place place;
    private Space studyRoom;
    private Space conferenceRoom;
    private Booker booker;

    @BeforeEach
    void setUp() {
        place = placeRepository.save(new Place(
                "Butler Library", "America/New_York",
                LocalTime.of(8, 0), LocalTime.of(20, 0), 24, 50));
        studyRoom = spaceRepository.save(new Space(
                place, "Study Room 101", 10, new BigDecimal("25.00"),
                Set.of()));
        conferenceRoom = spaceRepository.save(new Space(
                place, "Conference Room", 20, new BigDecimal("50.00"),
                Set.of()));
        booker = bookerRepository.save(new Booker("booker-1", "Alice"));

        reservationRepository.save(new Reservation(
                booker, studyRoom,
                LocalDateTime.of(2026, 11, 1, 10, 0),
                LocalDateTime.of(2026, 11, 1, 12, 0),
                4, new BigDecimal("50.00")));
        reservationRepository.save(new Reservation(
                booker, studyRoom,
                LocalDateTime.of(2026, 11, 1, 14, 0),
                LocalDateTime.of(2026, 11, 1, 16, 0),
                6, new BigDecimal("50.00")));
        reservationRepository.save(new Reservation(
                booker, conferenceRoom,
                LocalDateTime.of(2026, 11, 2, 9, 0),
                LocalDateTime.of(2026, 11, 2, 11, 0),
                10, new BigDecimal("100.00")));
        reservationRepository.flush();
    }

    private MockHttpServletRequestBuilder reportRequest(final Long placeId) {
        return get("/api/reports/spaces")
                .param("placeId", placeId.toString())
                .param("startDate", START.toString())
                .param("endDate", END.toString());
    }

    @Test
    void returnsAReportForAllSpacesInThePlace() throws Exception {
        mockMvc.perform(reportRequest(place.getPlaceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value(place.getPlaceId()))
                .andExpect(jsonPath("$.placeName").value("Butler Library"))
                .andExpect(jsonPath("$.startDate")
                        .value("2026-11-01T00:00:00"))
                .andExpect(jsonPath("$.endDate")
                        .value("2026-11-03T00:00:00"))
                .andExpect(jsonPath("$.spaces", hasSize(2)))
                .andExpect(jsonPath("$.totalRevenue").value(200.00));
    }

    @Test
    void returnsDetailedMetricsForASingleSpace() throws Exception {
        mockMvc.perform(reportRequest(place.getPlaceId())
                        .param("spaceId", studyRoom.getSpaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaces", hasSize(1)))
                .andExpect(jsonPath("$.spaces[0].spaceId")
                        .value(studyRoom.getSpaceId()))
                .andExpect(jsonPath("$.spaces[0].spaceName")
                        .value("Study Room 101"))
                .andExpect(jsonPath("$.spaces[0].totalReservations")
                        .value(2))
                .andExpect(jsonPath("$.spaces[0].totalBookedHours")
                        .value(4.0))
                .andExpect(jsonPath("$.spaces[0].averageDurationHours")
                        .value(2.0))
                .andExpect(jsonPath("$.spaces[0].utilizationPercentage")
                        .value(16.67))
                .andExpect(jsonPath("$.spaces[0].peakBookingHour")
                        .value("10:00"))
                .andExpect(jsonPath("$.spaces[0].peakBookingDate")
                        .value("2026-11-01"))
                .andExpect(jsonPath("$.spaces[0].averageGroupSize")
                        .value(5.0))
                .andExpect(jsonPath("$.spaces[0].averageCapacityUsed")
                        .value(50.0))
                .andExpect(jsonPath("$.spaces[0].totalRevenue")
                        .value(100.00));
    }

    @Test
    void returnsZeroMetricsForASpaceWithoutReservations() throws Exception {
        Space emptyRoom = spaceRepository.save(new Space(
                place, "Empty Room", 5, new BigDecimal("10.00"), Set.of()));

        mockMvc.perform(reportRequest(place.getPlaceId())
                        .param("spaceId", emptyRoom.getSpaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaces", hasSize(1)))
                .andExpect(jsonPath("$.spaces[0].totalReservations").value(0))
                .andExpect(jsonPath("$.spaces[0].totalBookedHours").value(0.0))
                .andExpect(jsonPath("$.spaces[0].peakBookingHour")
                        .value("N/A"))
                .andExpect(jsonPath("$.spaces[0].peakBookingDate")
                        .value("N/A"))
                .andExpect(jsonPath("$.spaces[0].averageCapacityUsed")
                        .value(0.0))
                .andExpect(jsonPath("$.spaces[0].totalRevenue").value(0))
                .andExpect(jsonPath("$.totalRevenue").value(0));
    }

    @Test
    void excludesCancelledReservationsFromTheReport() throws Exception {
        Reservation cancelled = new Reservation(
                booker, studyRoom,
                LocalDateTime.of(2026, 11, 1, 16, 0),
                LocalDateTime.of(2026, 11, 1, 18, 0),
                2, new BigDecimal("50.00"));
        cancelled.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.saveAndFlush(cancelled);

        mockMvc.perform(reportRequest(place.getPlaceId())
                        .param("spaceId", studyRoom.getSpaceId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spaces[0].totalReservations")
                        .value(2))
                .andExpect(jsonPath("$.spaces[0].totalBookedHours")
                        .value(4.0))
                .andExpect(jsonPath("$.spaces[0].totalRevenue")
                        .value(100.00));
    }

    @Test
    void rejectsARequestWithoutAPlaceId() throws Exception {
        mockMvc.perform(get("/api/reports/spaces")
                        .param("startDate", START.toString())
                        .param("endDate", END.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsARequestWithoutDates() throws Exception {
        mockMvc.perform(get("/api/reports/spaces")
                        .param("placeId", place.getPlaceId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAStartDateThatIsNotBeforeTheEndDate() throws Exception {
        mockMvc.perform(get("/api/reports/spaces")
                        .param("placeId", place.getPlaceId().toString())
                        .param("startDate", END.toString())
                        .param("endDate", START.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAnUnknownPlace() throws Exception {
        mockMvc.perform(reportRequest(999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnUnknownSpace() throws Exception {
        mockMvc.perform(reportRequest(place.getPlaceId())
                        .param("spaceId", "999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsASpaceThatBelongsToAnotherPlace() throws Exception {
        Place otherPlace = placeRepository.save(new Place(
                "Lerner Hall", "America/New_York",
                LocalTime.of(8, 0), LocalTime.of(20, 0), 24, 50));
        Space otherSpace = spaceRepository.save(new Space(
                otherPlace, "Lerner Room", 8, new BigDecimal("20.00"),
                Set.of()));

        mockMvc.perform(reportRequest(place.getPlaceId())
                        .param("spaceId", otherSpace.getSpaceId().toString()))
                .andExpect(status().isBadRequest());
    }
}
