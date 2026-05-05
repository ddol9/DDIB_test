package com.ddib.monolith.reservation.api.dto;

import com.ddib.monolith.reservation.domain.Reservation;

public record MyReservationDetailResponse(
        String performanceTitle,
        java.time.LocalDateTime performanceStartAt,
        String venueName,
        String seatLabel,
        String seatType,
        String reservationNo,
        String qrToken
) {

    public static MyReservationDetailResponse from(Reservation reservation) {
        return new MyReservationDetailResponse(
                reservation.getPerformanceTitle(),
                reservation.getPerformanceStartAt(),
                reservation.getVenueName(),
                reservation.getSeatLabel(),
                reservation.getSeatType(),
                reservation.getReservationNo(),
                java.util.UUID.randomUUID().toString()
        );
    }
}
