package com.ddib.monolith.reservation.api.dto;

import com.ddib.monolith.reservation.domain.Reservation;
import com.ddib.monolith.reservation.domain.ReservationStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record MyReservationListResponse(
        Long reservationId,
        String reservationNo,
        Long ticketId,
        Long performanceId,
        Long performanceOptionId,
        String performanceTitle,
        String performanceImg,
        String venueName,
        Long ownerUserId,
        LocalDateTime performanceStartAt,
        LocalDateTime performanceEndAt,
        ReservationStatus reservationStatus,
        String seatPos,
        String seatType,
        Integer seatNumber,
        Integer price,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static MyReservationListResponse from(Reservation reservation) {
        return MyReservationListResponse.builder()
                .reservationId(reservation.getReservationId())
                .reservationNo(reservation.getReservationNo())
                .ticketId(reservation.getTicketId())
                .performanceId(reservation.getPerformanceId())
                .performanceOptionId(reservation.getPerformanceOptionId())
                .performanceTitle(reservation.getPerformanceTitle())
                .performanceImg(reservation.getPerformanceImg())
                .venueName(reservation.getVenueName())
                .ownerUserId(reservation.getOwnerUserId())
                .performanceStartAt(reservation.getPerformanceStartAt())
                .performanceEndAt(reservation.getPerformanceEndAt())
                .reservationStatus(reservation.getReservationStatus())
                .seatPos(reservation.getSeatLabel() + "-" + reservation.getSeatNumber())
                .seatType(reservation.getSeatType())
                .seatNumber(reservation.getSeatNumber())
                .price(reservation.getPrice())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
