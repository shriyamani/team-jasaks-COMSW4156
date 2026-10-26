package com.jasaks.repository;

import com.jasaks.model.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for {@link Space} entities.
 */
public interface SpaceRepository extends JpaRepository<Space, Long> {
    /**
     * Returns all the spaces in a given place
     * (ex: all the rooms in a given building).
     *
     * @param placeId id of the place
     * @return the spaces in the place
     */
    List<Space> findByPlacePlaceId(Long placeId);
}
