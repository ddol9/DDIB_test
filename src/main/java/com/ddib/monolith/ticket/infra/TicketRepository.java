package com.ddib.monolith.ticket.infra;

import com.ddib.monolith.ticket.domain.Ticket;
import com.ddib.monolith.ticket.domain.TicketStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    boolean existsByPerformanceIdAndSeatIdAndStatusIn(Long performanceId, Long seatId, Collection<TicketStatus> statuses);

    Optional<Ticket> findByPaymentIdAndSeatId(Long paymentId, Long seatId);
}
