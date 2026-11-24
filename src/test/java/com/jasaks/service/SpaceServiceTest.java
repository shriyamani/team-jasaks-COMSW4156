package com.jasaks.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jasaks.dto.SpaceRequest;
import com.jasaks.dto.SpaceResponse;
import com.jasaks.dto.SpaceSearchRequest;
import com.jasaks.dto.SpaceSearchResponse;
import com.jasaks.model.Place;
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
import java.util.Set;
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
 * Unit tests for {@link SpaceService}.
 */
@ExtendWith(MockitoExtension.class)
public final class SpaceServiceTest {

    /** Identifier of the fixture place. */
    private static final Long PLACE_ID = 1L;

    /** Identifier of the fixture space. */
    private static final Long SPACE_ID = 10L;

    /** Hourly rate of the fixture space. */
    private static final BigDecimal RATE = new BigDecimal("20.00");

    /** First day used by deterministic search tests. */
    private static final LocalDateTime DAY =
            LocalDateTime.of(2030, 6, 10, 0, 0);

    /** Mocked space repository. */
    @Mock
    private SpaceRepository spaceRepository;

    /** Mocked place repository. */
    @Mock
    private PlaceRepository placeRepository;

    /** Mocked reservation repository. */
    @Mock
    private ReservationRepository reservationRepository;

    /** Service under test. */
    @InjectMocks
    private SpaceService spaceService;

    /** Fixture place. */
    private Place place;

    /** Fixture space. */
    private Space space;

    /**
     * Sets IDs with reflection because JPA entities have no ID setters.
     *
     * @param target the object whose field is set
     * @param fieldName the name of the field
     * @param value the value to assign
     * @throws Exception if the field cannot be set
     */
    private void setField(final Object target, final String fieldName,
                          final Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Builds a space request.
     *
     * @param name the space name
     * @param capacity the capacity
     * @param rate the hourly rate
     * @param amenities the amenities
     * @return the request
     */
    private SpaceRequest spaceRequest(final String name, final int capacity,
                                      final BigDecimal rate,
                                      final Set<String> amenities) {
        return new SpaceRequest(
                name, capacity, rate, amenities, PLACE_ID);
    }

    /**
     * Builds a search request for the fixture place.
     *
     * @param start the requested start
     * @param end the requested end
     * @param minCapacity the minimum capacity
     * @param amenities the required amenities
     * @return the search request
     */
    private SpaceSearchRequest searchRequest(
            final LocalDateTime start, final LocalDateTime end,
            final int minCapacity, final Set<String> amenities) {
        return new SpaceSearchRequest(
                PLACE_ID, start, end, minCapacity, amenities);
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

        space = new Space(place, "Room A", 6, RATE,
                new HashSet<>(Set.of("wifi", "tv")));
        setField(space, "spaceId", SPACE_ID);
    }

    /**
     * Tests for listing and retrieving spaces.
     */
    @Nested
    final class ReadOperations {

        /**
         * A null place ID is the valid partition that selects all spaces.
         */
        @Test
        public void testGetSpacesWithoutFilterUsesFindAll() {
            when(spaceRepository.findAll()).thenReturn(List.of(space));

            final List<SpaceResponse> result = spaceService.getSpaces(null);

            assertEquals(1, result.size());
            assertEquals("Room A", result.get(0).spaceName());
            verify(spaceRepository).findAll();
        }

        /**
         * A known place ID selects only that place's spaces.
         */
        @Test
        public void testGetSpacesWithPlaceIdUsesFilteredLookup() {
            when(spaceRepository.findByPlacePlaceId(PLACE_ID))
                    .thenReturn(List.of(space));

            final List<SpaceResponse> result =
                    spaceService.getSpaces(PLACE_ID);

            assertEquals(SPACE_ID, result.get(0).spaceId());
            verify(spaceRepository).findByPlacePlaceId(PLACE_ID);
        }

        /**
         * An unknown place ID is valid but yields an empty result.
         */
        @Test
        public void testGetSpacesWithUnknownPlaceIdReturnsEmpty() {
            when(spaceRepository.findByPlacePlaceId(999L))
                    .thenReturn(List.of());

            assertTrue(spaceService.getSpaces(999L).isEmpty());
        }

        /**
         * An existing space is mapped into the response record.
         */
        @Test
        public void testGetSpaceReturnsMappedResponse() {
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(space));

            final SpaceResponse result = spaceService.getSpace(SPACE_ID);

            assertEquals(SPACE_ID, result.spaceId());
            assertEquals(PLACE_ID, result.placeId());
            assertEquals("Butler Library", result.placeName());
            assertEquals(RATE, result.hourlyRate());
            assertEquals(Set.of("wifi", "tv"), result.amenities());
        }

        /**
         * A missing space ID is the invalid not-found partition.
         */
        @Test
        public void testGetSpaceMissingThrowsNotFound() {
            when(spaceRepository.findById(999L))
                    .thenReturn(Optional.empty());

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.getSpace(999L));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        }
    }

    /**
     * Tests for creating spaces.
     */
    @Nested
    final class CreateOperations {

        /**
         * A complete valid request is persisted and mapped.
         */
        @Test
        public void testCreateSpaceWithValidRequest() {
            when(placeRepository.findById(PLACE_ID))
                    .thenReturn(Optional.of(place));
            when(spaceRepository.save(any(Space.class)))
                    .thenAnswer(call -> call.getArgument(0));

            final SpaceResponse result = spaceService.createSpace(
                    spaceRequest("Room B", 8, RATE,
                            new HashSet<>(Set.of("wifi"))));

            assertEquals("Room B", result.spaceName());
            assertEquals(8, result.capacity());
            assertEquals(Set.of("wifi"), result.amenities());
            verify(placeRepository).findById(PLACE_ID);
            verify(spaceRepository).save(any(Space.class));
        }

        /**
         * Null amenities are the valid partition normalized to empty.
         */
        @Test
        public void testCreateSpaceWithNullAmenitiesStoresEmptySet() {
            when(placeRepository.findById(PLACE_ID))
                    .thenReturn(Optional.of(place));
            when(spaceRepository.save(any(Space.class)))
                    .thenAnswer(call -> call.getArgument(0));

            final SpaceResponse result = spaceService.createSpace(
                    spaceRequest("Room C", 2, RATE, null));

            assertTrue(result.amenities().isEmpty());
        }

        /**
         * Capacity one and zero rate are the lower valid boundaries.
         */
        @Test
        public void testCreateSpaceAtLowerValueBoundaries() {
            when(placeRepository.findById(PLACE_ID))
                    .thenReturn(Optional.of(place));
            when(spaceRepository.save(any(Space.class)))
                    .thenAnswer(call -> call.getArgument(0));

            final SpaceResponse result = spaceService.createSpace(
                    spaceRequest("Room D", 1, BigDecimal.ZERO, null));

            assertEquals(1, result.capacity());
            assertEquals(BigDecimal.ZERO, result.hourlyRate());
        }

        /**
         * Invalid DTO values are not rejected by the service layer.
         */
        @Test
        public void testCreateSpaceLeavesBeanValidationToController() {
            when(placeRepository.findById(PLACE_ID))
                    .thenReturn(Optional.of(place));
            when(spaceRepository.save(any(Space.class)))
                    .thenAnswer(call -> call.getArgument(0));

            final SpaceResponse result = spaceService.createSpace(
                    spaceRequest("Room E", 0,
                            new BigDecimal("-1.00"), null));

            assertEquals(0, result.capacity());
            assertEquals(new BigDecimal("-1.00"), result.hourlyRate());
        }

        /**
         * An unknown place ID is the invalid lookup partition.
         */
        @Test
        public void testCreateSpaceWithUnknownPlaceThrowsNotFound() {
            when(placeRepository.findById(999L))
                    .thenReturn(Optional.empty());

            final SpaceRequest request = new SpaceRequest(
                    "Room F", 4, RATE, null, 999L);
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.createSpace(request));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
            verify(spaceRepository, never()).save(any(Space.class));
        }

        /**
         * A null request is outside the service contract.
         */
        @Test
        public void testCreateSpaceWithNullRequestThrows() {
            assertThrows(NullPointerException.class,
                    () -> spaceService.createSpace(null));
        }
    }

    /**
     * Tests for replacing spaces.
     */
    @Nested
    final class UpdateOperations {

        /**
         * All mutable fields are replaced and saved for a valid update.
         */
        @Test
        public void testUpdateSpaceReplacesAllFields() {
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(space));
            when(placeRepository.findById(PLACE_ID))
                    .thenReturn(Optional.of(place));
            when(spaceRepository.save(any(Space.class)))
                    .thenAnswer(call -> call.getArgument(0));

            final SpaceResponse result = spaceService.updateSpace(
                    SPACE_ID, spaceRequest("Room G", 12,
                            new BigDecimal("30.00"),
                            new HashSet<>(Set.of("tv"))));

            assertEquals("Room G", result.spaceName());
            assertEquals(12, result.capacity());
            assertEquals(new BigDecimal("30.00"), result.hourlyRate());
            assertEquals(Set.of("tv"), result.amenities());
            verify(spaceRepository).save(space);
        }

        /**
         * A missing space is rejected before the request is read.
         */
        @Test
        public void testUpdateSpaceMissingSpaceThrowsNotFound() {
            when(spaceRepository.findById(999L))
                    .thenReturn(Optional.empty());

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.updateSpace(999L,
                            spaceRequest("Room H", 4, RATE, null)));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
            verify(placeRepository, never()).findById(any());
        }

        /**
         * An unknown place on update is the invalid lookup partition.
         */
        @Test
        public void testUpdateSpaceWithUnknownPlaceThrowsNotFound() {
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(space));
            when(placeRepository.findById(999L))
                    .thenReturn(Optional.empty());

            final SpaceRequest request = new SpaceRequest(
                    "Room I", 4, RATE, null, 999L);
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.updateSpace(SPACE_ID, request));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
            verify(spaceRepository, never()).save(any(Space.class));
        }
    }

    /**
     * Tests for deleting spaces.
     */
    @Nested
    final class DeleteOperations {

        /**
         * No reservations is the valid partition that allows deletion.
         */
        @Test
        public void testDeleteSpaceWithoutReservations() {
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(space));
            when(reservationRepository.existsBySpaceSpaceId(SPACE_ID))
                    .thenReturn(false);

            assertDoesNotThrow(() -> spaceService.deleteSpace(SPACE_ID));

            verify(spaceRepository).delete(space);
        }

        /**
         * Any reservation is the invalid partition that blocks deletion.
         */
        @Test
        public void testDeleteSpaceWithReservationsThrowsConflict() {
            when(spaceRepository.findById(SPACE_ID))
                    .thenReturn(Optional.of(space));
            when(reservationRepository.existsBySpaceSpaceId(SPACE_ID))
                    .thenReturn(true);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.deleteSpace(SPACE_ID));

            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
            verify(spaceRepository, never()).delete(any(Space.class));
        }

        /**
         * A missing space cannot be deleted.
         */
        @Test
        public void testDeleteSpaceMissingThrowsNotFound() {
            when(spaceRepository.findById(999L))
                    .thenReturn(Optional.empty());

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.deleteSpace(999L));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
            verify(reservationRepository, never())
                    .existsBySpaceSpaceId(any());
        }
    }

    /**
     * Tests for search validation, filtering, and pricing.
     */
    @Nested
    final class SearchOperations {

        /**
         * A null start time is invalid.
         */
        @Test
        public void testSearchRejectsNullStartTime() {
            final SpaceSearchRequest request = searchRequest(
                    null, DAY.plusHours(2), 0, null);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }

        /**
         * A null end time is invalid.
         */
        @Test
        public void testSearchRejectsNullEndTime() {
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), null, 0, null);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }

        /**
         * Equal times are the invalid boundary of the ordering rule.
         */
        @Test
        public void testSearchRejectsEqualTimes() {
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(8), 0, null);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }

        /**
         * A reversed window is invalid.
         */
        @Test
        public void testSearchRejectsReversedTimes() {
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(9), DAY.plusHours(8), 0, null);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }

        /**
         * Exactly 24 hours is accepted by the duration check.
         */
        @Test
        public void testSearchAcceptsTwentyFourHourBoundary() {
            stubSearchPlace();
            when(spaceRepository.findByPlacePlaceId(PLACE_ID))
                    .thenReturn(List.of());
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(32), 0, null);

            assertTrue(spaceService.findAvailableSpaces(request).isEmpty());
        }

        /**
         * Twenty-five hours is the first rejected duration partition.
         */
        @Test
        public void testSearchRejectsTwentyFiveHourDuration() {
            stubSearchPlace();
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(33), 0, null);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.UNPROCESSABLE_CONTENT,
                    error.getStatusCode());
        }

        /**
         * A start before opening is outside the operating-hours partition.
         */
        @Test
        public void testSearchRejectsStartBeforeOpening() {
            stubSearchPlace();
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8).minusSeconds(1),
                    DAY.plusHours(10), 0, null);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.UNPROCESSABLE_CONTENT,
                    error.getStatusCode());
        }

        /**
         * An end after closing is outside the operating-hours partition.
         */
        @Test
        public void testSearchRejectsEndAfterClosing() {
            stubSearchPlace();
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(10),
                    DAY.plusHours(20).plusSeconds(1), 0, null);

            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.UNPROCESSABLE_CONTENT,
                    error.getStatusCode());
        }

        /**
         * Opening and closing times are inclusive boundaries.
         */
        @Test
        public void testSearchAcceptsOperatingHourBoundaries() {
            stubSearchPlace();
            when(spaceRepository.findByPlacePlaceId(PLACE_ID))
                    .thenReturn(List.of());
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(20), 0, null);

            assertTrue(spaceService.findAvailableSpaces(request).isEmpty());
        }

        /**
         * An unknown place is the invalid lookup partition.
         */
        @Test
        public void testSearchWithUnknownPlaceThrowsNotFound() {
            when(placeRepository.findById(999L))
                    .thenReturn(Optional.empty());

            final SpaceSearchRequest request = new SpaceSearchRequest(
                    999L, DAY.plusHours(8), DAY.plusHours(10), 0, null);
            final ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> spaceService.findAvailableSpaces(request));

            assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        }

        /**
         * Capacity equal to the space capacity is included.
         */
        @Test
        public void testSearchIncludesSpaceAtCapacityBoundary() {
            stubSearchPlace();
            stubAvailableSpace();
            stubOverlap(false);
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(10), 6, null);

            final List<SpaceSearchResponse> result =
                    spaceService.findAvailableSpaces(request);

            assertEquals(1, result.size());
        }

        /**
         * Capacity above the space capacity is excluded.
         */
        @Test
        public void testSearchExcludesSpaceAboveCapacityBoundary() {
            stubSearchPlace();
            stubAvailableSpace();
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(10), 7, null);

            assertTrue(spaceService.findAvailableSpaces(request).isEmpty());
        }

        /**
         * A required amenity that is present includes the space.
         */
        @Test
        public void testSearchIncludesSpaceWithRequiredAmenity() {
            stubSearchPlace();
            stubAvailableSpace();
            stubOverlap(false);
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(10), 0,
                    Set.of("wifi"));

            assertEquals(
                    1, spaceService.findAvailableSpaces(request).size());
        }

        /**
         * A missing amenity excludes the space.
         */
        @Test
        public void testSearchExcludesSpaceWithoutRequiredAmenity() {
            stubSearchPlace();
            stubAvailableSpace();
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(10), 0,
                    Set.of("printer"));

            assertTrue(spaceService.findAvailableSpaces(request).isEmpty());
        }

        /**
         * Request amenities are lowercased before matching.
         */
        @Test
        public void testSearchNormalizesAmenityCase() {
            stubSearchPlace();
            stubAvailableSpace();
            stubOverlap(false);
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(10), 0,
                    Set.of("WIFI"));

            assertEquals(
                    1, spaceService.findAvailableSpaces(request).size());
        }

        /**
         * An overlapping reservation excludes the space.
         */
        @Test
        public void testSearchExcludesOverlappingSpace() {
            stubSearchPlace();
            stubAvailableSpace();
            stubOverlap(true);
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(10), 0, null);

            assertTrue(spaceService.findAvailableSpaces(request).isEmpty());
            verify(reservationRepository).hasOverlap(
                    SPACE_ID, request.startTime(), request.endTime());
        }

        /**
         * A four-hour search is priced at the hourly rate.
         */
        @Test
        public void testSearchCalculatesFourHourPrice() {
            stubSearchPlace();
            stubAvailableSpace();
            stubOverlap(false);
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(12), 0, null);

            final List<SpaceSearchResponse> result =
                    spaceService.findAvailableSpaces(request);

            assertEquals(
                    new BigDecimal("80.00"), result.get(0).estPrice());
        }

        /**
         * A five-minute search exercises half-up rounding.
         */
        @Test
        public void testSearchRoundsFiveMinutePrice() {
            stubSearchPlace();
            stubAvailableSpace();
            stubOverlap(false);
            final SpaceSearchRequest request = searchRequest(
                    DAY.plusHours(8), DAY.plusHours(8).plusMinutes(5),
                    0, null);

            final List<SpaceSearchResponse> result =
                    spaceService.findAvailableSpaces(request);

            assertEquals(
                    new BigDecimal("1.67"), result.get(0).estPrice());
        }

        /**
         * Stubs the place lookup used by search tests.
         */
        private void stubSearchPlace() {
            when(placeRepository.findById(PLACE_ID))
                    .thenReturn(Optional.of(place));
        }

        /**
         * Stubs one candidate space and its overlap flag.
         *
         * @param overlaps whether the space has an overlapping reservation
         */
        private void stubAvailableSpace() {
            when(spaceRepository.findByPlacePlaceId(PLACE_ID))
                    .thenReturn(List.of(space));
        }

        /**
         * Stubs the overlap flag for tests that reach that filter.
         *
         * @param overlaps whether the space has an overlapping reservation
         */
        private void stubOverlap(final boolean overlaps) {
            when(reservationRepository.hasOverlap(any(), any(), any()))
                    .thenReturn(overlaps);
        }
    }

    /**
     * Tests for place-name lookup.
     */
    @Nested
    final class PlaceLookupOperations {

        /**
         * A known name returns its identifier.
         */
        @Test
        public void testFindPlaceIdByNameReturnsId() {
            when(placeRepository.findIdByName("Butler Library"))
                    .thenReturn(PLACE_ID);

            assertEquals(PLACE_ID,
                    spaceService.findPlaceIdByName("Butler Library"));
        }

        /**
         * An unknown name returns null rather than throwing.
         */
        @Test
        public void testFindPlaceIdByNameReturnsNullWhenMissing() {
            when(placeRepository.findIdByName("Unknown"))
                    .thenReturn(null);

            assertNull(spaceService.findPlaceIdByName("Unknown"));
        }
    }
}
