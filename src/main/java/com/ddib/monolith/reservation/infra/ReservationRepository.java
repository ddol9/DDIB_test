package com.ddib.monolith.reservation.infra;

import com.ddib.monolith.reservation.domain.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findAllByOwnerUserIdOrderByReservationIdDesc(Long ownerUserId, Pageable pageable);

    List<Reservation> findAllByOwnerUserIdAndReservationIdLessThanOrderByReservationIdDesc(
            Long ownerUserId,
            Long cursor,
            Pageable pageable
    );

    @Query("""
            select r from Reservation r
            where r.ownerUserId = :ownerUserId
              and r.performanceStartAt > :now
              and (:cursor is null or r.reservationId < :cursor)
            order by r.reservationId desc
            """)
    List<Reservation> findUpcomingTickets(
            @Param("ownerUserId") Long ownerUserId,
            @Param("now") LocalDateTime now,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            select r from Reservation r
            where r.ownerUserId = :ownerUserId
              and r.performanceEndAt < :now
              and (:cursor is null or r.reservationId < :cursor)
            order by r.reservationId desc
            """)
    List<Reservation> findPastTickets(
            @Param("ownerUserId") Long ownerUserId,
            @Param("now") LocalDateTime now,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    Optional<Reservation> findByReservationIdAndOwnerUserId(Long reservationId, Long ownerUserId);

    Optional<Reservation> findByTicketId(Long ticketId);
}
