package com.ddib.monolith.reservation.api;

import com.ddib.monolith.reservation.api.dto.CursorPageResponse;
import com.ddib.monolith.reservation.api.dto.MyReservationDetailResponse;
import com.ddib.monolith.reservation.api.dto.MyReservationListResponse;
import com.ddib.monolith.reservation.application.ReservationService;
import com.ddib.monolith.reservation.domain.TicketType;
import com.ddib.monolith.support.security.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/my")
    public ResponseEntity<CursorPageResponse<MyReservationListResponse>> getMyReservations(
            @UserId Long userId,
            @RequestParam(defaultValue = "ALL") TicketType type,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reservationService.getMyReservations(userId, type, cursor, size));
    }

    @GetMapping("/my/{reservationId}")
    public ResponseEntity<MyReservationDetailResponse> getMyReservationDetail(
            @UserId Long userId,
            @PathVariable Long reservationId
    ) {
        return ResponseEntity.ok(reservationService.getMyReservationDetail(userId, reservationId));
    }
}
