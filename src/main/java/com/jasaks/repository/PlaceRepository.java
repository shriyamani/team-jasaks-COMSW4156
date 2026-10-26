package com.jasaks.repository;

import com.jasaks.model.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    @Query("SELECT p.id FROM Place p WHERE p.placeName = :placeName")
    Long findIdByName(@Param("placeName") String placeName);
}
