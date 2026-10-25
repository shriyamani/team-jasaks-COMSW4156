package com.jasaks.repository;

import com.jasaks.model.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository interface for managing Reservation entities.
 */
public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    /**
     * Checks if any reservation exists for a given space ID.
     *
     * @param spaceId the ID of the space
     * @return true if a reservation exists, false otherwise
     */
    boolean existsBySpace_SpaceId(Long spaceId);

    /**
     * Checks if there is an overlapping reservation for a space.
     *
     * @param spaceId the ID of the space
     * @param start the start time
     * @param end the end time
     * @return true if an overlap exists, false otherwise
     */
    @Query("""
        SELECT COUNT(r) > 0 from Reservation r
        WHERE r.space.spaceId = :spaceId
            AND r.status <> com.jasaks.model.ReservationStatus.CANCELLED
            AND r.startTime < :end
            AND r.endTime > :start
    """)
    boolean hasOverlap(
            @Param("spaceId") Long spaceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Checks for overlapping reservations excluding a specific reservation.
     *
     * @param spaceId the ID of the space
     * @param start the start time
     * @param end the end time
     * @param excludeId the reservation ID to exclude
     * @return true if an overlap exists, false otherwise
     */
    @Query("""
        SELECT COUNT(r) > 0 from Reservation r
        WHERE r.space.spaceId = :spaceId
            AND r.reservationId <> :excludeId
            AND r.status <> com.jasaks.model.ReservationStatus.CANCELLED
            AND r.startTime < :end
            AND r.endTime > :start
    """)
    boolean hasOverlapExcluding(
            @Param("spaceId") Long spaceId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId);

    /**
     * Finds all reservations belonging to a specific booker.
     *
     * @param bookerId the ID of the booker
     * @return list of reservations for the booker
     */
    List<Reservation> findByBookerBookerId(String bookerId);
}
