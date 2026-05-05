package com.ddib.monolith.ticket.application;

import com.ddib.monolith.payment.domain.event.PaymentSucceededEvent;
import com.ddib.monolith.performance.domain.Performance;
import com.ddib.monolith.performance.domain.PerformanceOption;
import com.ddib.monolith.performance.domain.Seat;
import com.ddib.monolith.performance.domain.SeatTypePrice;
import com.ddib.monolith.performance.infra.PerformanceOptionRepository;
import com.ddib.monolith.performance.infra.PerformanceRepository;
import com.ddib.monolith.performance.infra.SeatRepository;
import com.ddib.monolith.performance.infra.SeatTypePriceRepository;
import com.ddib.monolith.queue.application.QueueService;
import com.ddib.monolith.seat.application.BroadcastService;
import com.ddib.monolith.seat.domain.SeatBroadcastMessage;
import com.ddib.monolith.seat.domain.SeatMessageType;
import com.ddib.monolith.seat.domain.SeatStore;
import com.ddib.monolith.ticket.domain.Ticket;
import com.ddib.monolith.ticket.domain.TicketStatus;
import com.ddib.monolith.ticket.domain.event.TicketIssuedEvent;
import com.ddib.monolith.ticket.infra.TicketRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketIssueService {

    private static final EnumSet<TicketStatus> ACTIVE_STATUSES = EnumSet.of(
            TicketStatus.ISSUED,
            TicketStatus.USED,
            TicketStatus.EXPIRED
    );

    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceOptionRepository performanceOptionRepository;
    private final SeatTypePriceRepository seatTypePriceRepository;
    private final SeatStore seatStore;
    private final QueueService queueService;
    private final BroadcastService broadcastService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int issueTickets(PaymentSucceededEvent event) {
        Performance performance = performanceRepository.findById(event.performanceId()).orElseThrow();
        PerformanceOption option = performanceOptionRepository.findById(event.optionId()).orElseThrow();
        List<Long> soldSeatIds = new ArrayList<>();

        for (String seatIdValue : event.seatIds()) {
            Long seatId = Long.parseLong(seatIdValue);
            if (ticketRepository.existsByPerformanceIdAndSeatIdAndStatusIn(event.performanceId(), seatId, ACTIVE_STATUSES)) {
                continue;
            }

            Seat seat = seatRepository.findById(seatId).orElseThrow();
            SeatTypePrice seatTypePrice = seatTypePriceRepository.findByPerformanceIdAndSeatLabel(event.performanceId(), seat.getSeatLabel())
                    .orElseThrow();

            Ticket ticket = ticketRepository.save(Ticket.create(
                    event.userId(),
                    event.paymentId(),
                    seatTypePrice.getPrice(),
                    event.performanceId(),
                    event.optionId(),
                    seatId,
                    seatTypePrice.getSeatType()
            ));

            seatStore.releaseSeat(event.performanceId(), event.optionId(), seatId, event.userId());
            seatStore.addSoldSeats(event.performanceId(), event.optionId(), List.of(seatId));
            soldSeatIds.add(seatId);
            eventPublisher.publishEvent(new TicketIssuedEvent(
                    UUID.randomUUID().toString(),
                    "TICKET_ISSUED",
                    Instant.now().toString(),
                    event.traceId(),
                    ticket.getId(),
                    ticket.getOwnerUserId(),
                    ticket.getStatus().name(),
                    performance.getId(),
                    option.getId(),
                    performance.getTitle(),
                    performance.getPerformanceImg(),
                    performance.getVenue().getName(),
                    seat.getId(),
                    seat.getSeatLabel(),
                    ticket.getSeatType(),
                    seat.getSeatNumber(),
                    ticket.getPrice(),
                    option.getStartAt().toString(),
                    performance.getEndDate().atTime(23, 59, 59).toString()
            ));
        }

        queueService.revokeToken(event.performanceId(), event.optionId(), event.tokenId());
        if (!soldSeatIds.isEmpty()) {
            broadcastService.broadcastToRoom(
                    event.performanceId(),
                    event.optionId(),
                    SeatBroadcastMessage.builder()
                            .type(SeatMessageType.SEAT_SOLD)
                            .performanceId(event.performanceId())
                            .optionId(event.optionId())
                            .seatIds(soldSeatIds)
                            .timestamp(Instant.now().toString())
                            .build()
            );
        }
        return soldSeatIds.size();
    }
}
