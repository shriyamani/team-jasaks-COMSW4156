package com.jasaks.controller;

import com.jasaks.dto.SpaceRequest;
import com.jasaks.dto.SpaceResponse;
import com.jasaks.dto.SpaceSearchRequest;
import com.jasaks.dto.SpaceSearchResponse;
import com.jasaks.model.Space;
import com.jasaks.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for managing spaces.
 */
@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    /** Service used to manage spaces. */
    private final SpaceService spaceService;

    /**
     * Creates a controller backed by the given service.
     *
     * @param spaceService service used to manage spaces
     */
    public SpaceController(final SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    /**
     * Lists spaces, optionally restricted to a single place.
     *
     * @param placeId optional place identifier to filter by
     * @return spaces matching the filter
     */
    @GetMapping
    public List<SpaceResponse> getSpaces(
            @RequestParam(required = false) final Long placeId) {
        return spaceService.getSpaces(placeId);
    }

    /**
     * Returns a single space.
     *
     * @param spaceId identifier of the space
     * @return the requested space
     */
    @GetMapping("/{spaceId}")
    public SpaceResponse getSpace(@PathVariable final Long spaceId) {
        return spaceService.getSpace(spaceId);
    }

    /**
     * Creates a new space.
     *
     * @param request details of the space to create
     * @return the created space
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponse createSpace(
            @Valid @RequestBody final SpaceRequest request) {
        return spaceService.createSpace(request);
    }

    /**
     * Replaces an existing space.
     *
     * @param spaceId identifier of the space to update
     * @param request new details for the space
     * @return the updated space
     */
    @PutMapping("/{spaceId}")
    public SpaceResponse updateSpace(
            @PathVariable final Long spaceId,
            @Valid @RequestBody final SpaceRequest request) {
        return spaceService.updateSpace(spaceId, request);
    }

    /**
     * Deletes a space that has no reservations.
     *
     * @param spaceId identifier of the space to delete
     */
    @DeleteMapping("/{spaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSpace(@PathVariable final Long spaceId) {
        spaceService.deleteSpace(spaceId);
    }

    /**
     * Gets available spaces for a given place.
     *
     * @param request the search available spaces request
     */
    @GetMapping("/{placeId}")
    public ResponseEntity<List<SpaceSearchResponse>> getAvailableSpaces(
            @RequestBody final SpaceSearchRequest request) {
        return spaceService.findAvailableSpaces(request);
    }
}
