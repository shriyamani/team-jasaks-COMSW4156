package com.jasaks.service;

import com.jasaks.dto.SpaceRequest;
import com.jasaks.dto.SpaceResponse;
import com.jasaks.dto.SpaceSearchRequest;
import com.jasaks.dto.SpaceSearchResponse;
import com.jasaks.model.Place;
import com.jasaks.model.Space;
import com.jasaks.repository.PlaceRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.Duration;

@Service
public class SpaceService {

    /** Repository for space persistence. */
    private final SpaceRepository spaceRepository;

    /** Repository for place lookups. */
    private final PlaceRepository placeRepository;

    /** Repository for reservation lookups. */
    private final ReservationRepository reservationRepository;

    /**
     * Creates a service backed by the given repositories.
     *
     * @param spaceRepository repository for spaces
     * @param placeRepository repository for places
     * @param reservationRepository repository for reservations
     */
    public SpaceService(final SpaceRepository spaceRepository,
                        final PlaceRepository placeRepository,
                        final ReservationRepository reservationRepository) {
        this.spaceRepository = spaceRepository;
        this.placeRepository = placeRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Lists spaces, optionally filtered by place.
     *
     * @param placeId optional place identifier to filter by
     * @return spaces matching the filter
     */
    @Transactional(readOnly = true)
    public List<SpaceResponse> getSpaces(final Long placeId) {
        List<Space> spaces = placeId == null
                ? spaceRepository.findAll()
                : spaceRepository.findByPlace_PlaceId(placeId);

        return spaces.stream()
                .map(SpaceResponse::from)
                .toList();
    }

    /**
     * Returns a single space.
     *
     * @param spaceId identifier of the space
     * @return the requested space
     */
    @Transactional(readOnly = true)
    public SpaceResponse getSpace(final Long spaceId) {
        return SpaceResponse.from(findSpace(spaceId));
    }

    /**
     * Creates a space.
     *
     * @param request details of the space to create
     * @return the created space
     */
    @Transactional
    public SpaceResponse createSpace(final SpaceRequest request) {
        Place place = findPlace(request.placeId());

        Space space = new Space(
                place,
                request.spaceName(),
                request.capacity(),
                request.hourlyRate(),
                request.amenities()
        );

        return SpaceResponse.from(spaceRepository.save(space));
    }

    /**
     * Replaces an existing space.
     *
     * @param spaceId identifier of the space to update
     * @param request new details for the space
     * @return the updated space
     */
    @Transactional
    public SpaceResponse updateSpace(final Long spaceId,
                                     final SpaceRequest request) {
        Space space = findSpace(spaceId);
        Place place = findPlace(request.placeId());

        space.setPlace(place);
        space.setSpaceName(request.spaceName());
        space.setCapacity(request.capacity());
        space.setHourlyRate(request.hourlyRate());
        space.setAmenities(request.amenities());

        return SpaceResponse.from(spaceRepository.save(space));
    }

    /**
     * Deletes a space that has no reservations.
     *
     * @param spaceId identifier of the space to delete
     */
    @Transactional
    public void deleteSpace(final Long spaceId) {
        Space space = findSpace(spaceId);

        if (reservationRepository.existsBySpaceSpaceId(spaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Space " + spaceId
                            + " has reservations and cannot be deleted"
            );
        }

        spaceRepository.delete(space);
    }

    private Space findSpace(final Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Space not found: " + spaceId
                ));
    }

    private Place findPlace(final Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Place not found: " + placeId
                ));
    }

    public List<SpaceSearchResponse> findAvailableSpaces(final SpaceSearchRequest request) {

        findPlace(request.placeId());

        List<Space> spaces = spaceRepository.findByPlace_PlaceId(request.placeId());
        List<SpaceSearchResponse> available = new ArrayList<SpaceSearchResponse>();
        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime();
        Duration duration = Duration.between(start, end);

        for (Space space: spaces) {
            if (!request.amenities().isEmpty()) {
                Set<String> availableAmenities = space.getAmenities();
                if (!availableAmenities.containsAll(request.amenities())) {
                    spaces.remove(space);
                    continue;
                }
            }

            if (space.getCapacity() < request.minCapacity()) {
                spaces.remove(space);
                continue;
            }

            if (reservationRepository.hasOverlap(space.getSpaceId(), request.startTime(), request.endTime())) {
                spaces.remove(space);
            }

            BigDecimal hours = BigDecimal.valueOf(duration.toHoursPart());
            BigDecimal price = space.getHourlyRate().multiply(hours);

            SpaceSearchResponse response  = new SpaceSearchResponse(space, price);
            available.add(response);
        }
        return available;
    }
}
