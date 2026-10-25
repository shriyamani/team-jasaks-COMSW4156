package com.jasaks.controller;

import com.jasaks.dto.SpaceRequest;
import com.jasaks.dto.SpaceResponse;
import com.jasaks.service.SpaceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    /**
     * Lists spaces, optionally restricted to a single place.
     */
    @GetMapping
    public List<SpaceResponse> getSpaces(@RequestParam(required = false) Long placeId) {
        return spaceService.getSpaces(placeId);
    }

    @GetMapping("/{spaceId}")
    public SpaceResponse getSpace(@PathVariable Long spaceId) {
        return spaceService.getSpace(spaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponse createSpace(@Valid @RequestBody SpaceRequest request) {
        return spaceService.createSpace(request);
    }

    @PutMapping("/{spaceId}")
    public SpaceResponse updateSpace(@PathVariable Long spaceId,
                                     @Valid @RequestBody SpaceRequest request) {
        return spaceService.updateSpace(spaceId, request);
    }

    @DeleteMapping("/{spaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSpace(@PathVariable Long spaceId) {
        spaceService.deleteSpace(spaceId);
    }
}
