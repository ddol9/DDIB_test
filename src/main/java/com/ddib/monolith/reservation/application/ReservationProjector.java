package com.ddib.monolith.reservation.application;

import com.ddib.monolith.reservation.domain.Reservation;
import com.ddib.monolith.reservation.infra.ReservationRepository;
import com.ddib.monolith.ticket.domain.event.TicketIssuedEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservationProjector {

    private final ReservationRepository reservationRepository;
    private final ReservationNoGenerator reservationNoGenerator;

    @EventListener
    @Transactional
    public void handle(TicketIssuedEvent event) {
        if (reservationRepository.findByTicketId(event.ticketId()).isPresent()) {
            return;
        }
        reservationRepository.save(Reservation.create(
                reservationNoGenerator.generate(),
                event.ticketId(),
                event.seatId(),
                event.performanceId(),
                event.performanceOptionId(),
                event.performanceTitle(),
                event.performanceImg(),
                event.venueName(),
                event.ownerUserId(),
                LocalDateTime.parse(event.performanceStartAt()),
                LocalDateTime.parse(event.performanceEndAt()),
                event.seatLabel(),
                event.seatType(),
                event.seatNumber(),
                event.price()
        ));
    }
}
