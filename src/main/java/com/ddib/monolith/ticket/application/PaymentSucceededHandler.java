package com.ddib.monolith.ticket.application;

import com.ddib.monolith.payment.domain.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSucceededHandler {

    private final TicketIssueService ticketIssueService;

    @EventListener
    public void handle(PaymentSucceededEvent event) {
        ticketIssueService.issueTickets(event);
    }
}
