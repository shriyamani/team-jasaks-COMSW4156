package com.jasaks.repository;

import com.jasaks.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Repository for {@link Reservation} entities.
 */
public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {
    /**
     * Checks whether any non-cancelled reservation for the space overlaps
     * the given time period.
     *
     * @param spaceId id of the space
     * @param start start of the time period
     * @param end end of the time period
     * @return true if an overlapping reservation exists
     */
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

    /**
     * Checks whether any non-cancelled reservation for the space, other
     * than the excluded one, overlaps the given time period.
     *
     * @param spaceId id of the space
     * @param start start of the time period
     * @param end end of the time period
     * @param excludeId id of the reservation to ignore
     * @return true if an overlapping reservation exists
     */
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
}
