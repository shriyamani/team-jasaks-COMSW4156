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
     * Checks whether a space has any reservations.
     *
     * @param spaceId identifier of the space
     * @return {@code true} when at least one reservation exists
     */
    boolean existsBySpaceSpaceId(Long spaceId);

    /**
     * Checks whether a reservation overlaps an interval for a space.
     *
     * @param spaceId identifier of the space
     * @param start start of the interval
     * @param end end of the interval
     * @return {@code true} when an overlap exists
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
     * Checks whether another reservation overlaps an interval for a space.
     *
     * @param spaceId identifier of the space
     * @param start start of the interval
     * @param end end of the interval
     * @param excludeId identifier of the reservation to ignore
     * @return {@code true} when an overlap exists
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

    /**
     * Finds active reservations for a space overlapping a time window.
     *
     * @param spaceId the ID of the space
     * @param start the start of the time window
     * @param end the end of the time window
     * @return list of overlapping active reservations
     */
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

