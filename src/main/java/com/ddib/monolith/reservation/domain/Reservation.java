package com.ddib.monolith.reservation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation", indexes = {
        @Index(name = "idx_reservation_owner", columnList = "owner_user_id"),
        @Index(name = "idx_reservation_ticket", columnList = "ticket_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "reservation_no", nullable = false, unique = true)
    private String reservationNo;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private Long ticketId;

    @Column(name = "seat_id")
    private Long seatId;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(name = "performance_option_id", nullable = false)
    private Long performanceOptionId;

    @Column(name = "performance_title", nullable = false)
    private String performanceTitle;

    @Column(name = "performance_img", nullable = false)
    private String performanceImg;

    @Column(name = "venue_name", nullable = false)
    private String venueName;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "performance_start_at", nullable = false)
    private LocalDateTime performanceStartAt;

    @Column(name = "performance_end_at", nullable = false)
    private LocalDateTime performanceEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus reservationStatus;

    @Column(name = "seat_label", nullable = false)
    private String seatLabel;

    @Column(name = "seat_type", nullable = false)
    private String seatType;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Reservation(
            String reservationNo,
            Long ticketId,
            Long seatId,
            Long performanceId,
            Long performanceOptionId,
            String performanceTitle,
            String performanceImg,
            String venueName,
            Long ownerUserId,
            LocalDateTime performanceStartAt,
            LocalDateTime performanceEndAt,
            ReservationStatus reservationStatus,
            String seatLabel,
            String seatType,
            Integer seatNumber,
            Integer price
    ) {
        this.reservationNo = reservationNo;
        this.ticketId = ticketId;
        this.seatId = seatId;
        this.performanceId = performanceId;
        this.performanceOptionId = performanceOptionId;
        this.performanceTitle = performanceTitle;
        this.performanceImg = performanceImg;
        this.venueName = venueName;
        this.ownerUserId = ownerUserId;
        this.performanceStartAt = performanceStartAt;
        this.performanceEndAt = performanceEndAt;
        this.reservationStatus = reservationStatus;
        this.seatLabel = seatLabel;
        this.seatType = seatType;
        this.seatNumber = seatNumber;
        this.price = price;
    }

    public static Reservation create(
            String reservationNo,
            Long ticketId,
            Long seatId,
            Long performanceId,
            Long performanceOptionId,
            String performanceTitle,
            String performanceImg,
            String venueName,
            Long ownerUserId,
            LocalDateTime performanceStartAt,
            LocalDateTime performanceEndAt,
            String seatLabel,
            String seatType,
            Integer seatNumber,
            Integer price
    ) {
        return Reservation.builder()
                .reservationNo(reservationNo)
                .ticketId(ticketId)
                .seatId(seatId)
                .performanceId(performanceId)
                .performanceOptionId(performanceOptionId)
                .performanceTitle(performanceTitle)
                .performanceImg(performanceImg)
                .venueName(venueName)
                .ownerUserId(ownerUserId)
                .performanceStartAt(performanceStartAt)
                .performanceEndAt(performanceEndAt)
                .reservationStatus(ReservationStatus.ISSUED)
                .seatLabel(seatLabel)
                .seatType(seatType)
                .seatNumber(seatNumber)
                .price(price)
                .build();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
