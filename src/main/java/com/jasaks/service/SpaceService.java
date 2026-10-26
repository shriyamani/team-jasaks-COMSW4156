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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    /**
     * Searches for available spaces that match given parameters.
     *
     * @param request the object representing the search request parameters
     */
    public List<SpaceSearchResponse> findAvailableSpaces(final SpaceSearchRequest request) {

        if (request.startTime() == null || request.endTime() == null
                || !request.startTime().isBefore(request.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "startTime must be before endTime");
        }

        Place searchedPlace = findPlace(request.placeId());
        LocalTime openTime = searchedPlace.getOpenTime();
        LocalTime closeTime = searchedPlace.getCloseTime();

        int totalHours = BigDecimal.valueOf(Duration.between(request.startTime(), request.endTime()).toHours()).intValueExact();

        if (totalHours > 24) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "startTime and endTime duration must be less than 24 hours");
        }

        if (request.startTime().toLocalTime().isBefore(openTime) ||
                request.endTime().toLocalTime().isAfter(closeTime)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "startTime and endTime must be between " + openTime + "and " + closeTime + ".");
        }

        List<Space> spaces = spaceRepository.findByPlace_PlaceId(request.placeId());
        List<SpaceSearchResponse> available = new ArrayList<>();
        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime();
        BigDecimal minutes = BigDecimal.valueOf(Duration.between(start, end).toMinutes()).divide(BigDecimal.valueOf(60));

        for (Space space: spaces) {
            if (!request.amenities().isEmpty()) {
                Set<String> availableAmenities = space.getAmenities();
                if (!availableAmenities.containsAll(request.amenities())) {
                    continue;
                }
            }

            if (space.getCapacity() < request.minCapacity()) {
                continue;
            }

            if (reservationRepository.hasOverlap(space.getSpaceId(), request.startTime(), request.endTime())) {
                continue;
            }

            BigDecimal hourlyRate = space.getHourlyRate();
            BigDecimal price = hourlyRate.multiply(minutes).setScale(2, RoundingMode.HALF_UP);

            SpaceSearchResponse response  = new SpaceSearchResponse(SpaceResponse.from(space), price);
            available.add(response);
        }
        return available;
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

    public Long findPlaceIdByName(final String placeName) {
        return placeRepository.findIdByName(placeName);
    }


}
