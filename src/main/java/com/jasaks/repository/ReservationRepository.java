package com.jasaks.repository;

import com.jasaks.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @Query("""
        SELECT COUNT(r) > 0 from Reservation r
        WHERE r.space.spaceId = :spaceId
            AND r.status <> com.jasaks.model.ReservationStatus.CANCELLED
            AND r.startTime < :end
            AND r.endTime > :start
    """)
    boolean hasOverlap(@Param("spaceId") Long spaceId,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);
}
