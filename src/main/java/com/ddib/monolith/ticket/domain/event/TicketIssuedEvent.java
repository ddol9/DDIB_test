package com.ddib.monolith.ticket.domain.event;

public record TicketIssuedEvent(
        String eventId,
        String eventType,
        String occurredAt,
        String traceId,
        Long ticketId,
        Long ownerUserId,
        String status,
        Long performanceId,
        Long performanceOptionId,
        String performanceTitle,
        String performanceImg,
        String venueName,
        Long seatId,
        String seatLabel,
        String seatType,
        Integer seatNumber,
        Integer price,
        String performanceStartAt,
        String performanceEndAt
) {
}
