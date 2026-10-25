package com.jasaks.service;

import com.jasaks.dto.SpaceRequest;
import com.jasaks.dto.SpaceResponse;
import com.jasaks.model.Place;
import com.jasaks.model.Space;
import com.jasaks.repository.PlaceRepository;
import com.jasaks.repository.ReservationRepository;
import com.jasaks.repository.SpaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final PlaceRepository placeRepository;
    private final ReservationRepository reservationRepository;

    public SpaceService(SpaceRepository spaceRepository,
                        PlaceRepository placeRepository,
                        ReservationRepository reservationRepository) {
        this.spaceRepository = spaceRepository;
        this.placeRepository = placeRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> getSpaces(Long placeId) {
        List<Space> spaces = placeId == null
                ? spaceRepository.findAll()
                : spaceRepository.findByPlace_PlaceId(placeId);

        return spaces.stream()
                .map(SpaceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpaceResponse getSpace(Long spaceId) {
        return SpaceResponse.from(findSpace(spaceId));
    }

    @Transactional
    public SpaceResponse createSpace(SpaceRequest request) {
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

    @Transactional
    public SpaceResponse updateSpace(Long spaceId, SpaceRequest request) {
        Space space = findSpace(spaceId);
        Place place = findPlace(request.placeId());

        space.setPlace(place);
        space.setSpaceName(request.spaceName());
        space.setCapacity(request.capacity());
        space.setHourlyRate(request.hourlyRate());
        space.setAmenities(request.amenities());

        return SpaceResponse.from(spaceRepository.save(space));
    }

    @Transactional
    public void deleteSpace(Long spaceId) {
        Space space = findSpace(spaceId);

        if (reservationRepository.existsBySpaceSpaceId(spaceId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Space " + spaceId + " has reservations and cannot be deleted"
            );
        }

        spaceRepository.delete(space);
    }

    private Space findSpace(Long spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Space not found: " + spaceId
                ));
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Place not found: " + placeId
                ));
    }
}
