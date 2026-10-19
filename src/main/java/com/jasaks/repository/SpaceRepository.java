package com.jasaks.repository;

import com.jasaks.model.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpaceRepository extends JpaRepository<Space, Long> {
    //Returns all the spaces in a given place (ex: all the rooms in a given building)
    List<Space> findByPlace_PlaceId(Long placeId);
}
