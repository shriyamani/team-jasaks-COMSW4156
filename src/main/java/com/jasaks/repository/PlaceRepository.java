package com.jasaks.repository;

import com.jasaks.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for {@link Place} entities.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {

    /**
     * Finds the ID of a place by its name.
     *
     * @param placeName the name of the place
     * @return the place ID, or null if no place has that name
     */
    @Query("SELECT p.id FROM Place p WHERE p.placeName = :placeName")
    Long findIdByName(@Param("placeName") String placeName);
}
