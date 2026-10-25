package com.jasaks.repository;

import com.jasaks.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsBySpace_SpaceId(Long spaceId);

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

    @Query("""
        SELECT COUNT(r) > 0 from Reservation r
        WHERE r.space.spaceId = :spaceId
            AND r.reservationId <> :excludeId
            AND r.status <> com.jasaks.model.ReservationStatus.CANCELLED
            AND r.startTime < :end
            AND r.endTime > :start
    """)
    boolean hasOverlapExcluding(@Param("spaceId") Long spaceId,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end,
                       @Param("excludeId") Long excludeId);

    @Query("""
        SELECT r from Reservation r
        WHERE r.space.spaceId = :spaceId
            AND r.status <> com.jasaks.model.ReservationStatus.CANCELLED
            AND r.startTime < :end
            AND r.endTime > :start
    """)
    List<Reservation> findActiveReservationsInWindow(
            @Param("spaceId") Long spaceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
