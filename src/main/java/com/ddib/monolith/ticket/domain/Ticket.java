package com.ddib.monolith.ticket.domain;

import com.ddib.monolith.performance.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(name = "performance_option_id", nullable = false)
    private Long performanceOptionId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "seat_type", nullable = false)
    private String seatType;

    @Builder(access = AccessLevel.PRIVATE)
    private Ticket(
            Long ownerUserId,
            Long paymentId,
            TicketStatus status,
            Integer price,
            Long performanceId,
            Long performanceOptionId,
            Long seatId,
            String seatType
    ) {
        this.ownerUserId = ownerUserId;
        this.paymentId = paymentId;
        this.status = status;
        this.price = price;
        this.performanceId = performanceId;
        this.performanceOptionId = performanceOptionId;
        this.seatId = seatId;
        this.seatType = seatType;
    }

    public static Ticket create(
            Long ownerUserId,
            Long paymentId,
            Integer price,
            Long performanceId,
            Long performanceOptionId,
            Long seatId,
            String seatType
    ) {
        return Ticket.builder()
                .ownerUserId(ownerUserId)
                .paymentId(paymentId)
                .status(TicketStatus.ISSUED)
                .price(price)
                .performanceId(performanceId)
                .performanceOptionId(performanceOptionId)
                .seatId(seatId)
                .seatType(seatType)
                .build();
    }
}
