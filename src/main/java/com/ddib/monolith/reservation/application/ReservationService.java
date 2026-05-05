package com.ddib.monolith.reservation.application;

import com.ddib.monolith.reservation.api.dto.CursorPageResponse;
import com.ddib.monolith.reservation.api.dto.MyReservationDetailResponse;
import com.ddib.monolith.reservation.api.dto.MyReservationListResponse;
import com.ddib.monolith.reservation.domain.Reservation;
import com.ddib.monolith.reservation.domain.TicketType;
import com.ddib.monolith.reservation.exception.ReservationErrorCode;
import com.ddib.monolith.reservation.infra.ReservationRepository;
import com.ddib.monolith.support.exception.CustomException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public CursorPageResponse<MyReservationListResponse> getMyReservations(Long userId, TicketType type, Long cursor, int size) {
        int pageSize = Math.max(1, Math.min(size, 100));
        List<Reservation> reservations = switch (type) {
            case UPCOMING -> reservationRepository.findUpcomingTickets(
                    userId,
                    LocalDateTime.now(),
                    cursor,
                    PageRequest.of(0, pageSize + 1)
            );
            case PAST -> reservationRepository.findPastTickets(
                    userId,
                    LocalDateTime.now(),
                    cursor,
                    PageRequest.of(0, pageSize + 1)
            );
            case ALL -> cursor == null
                    ? reservationRepository.findAllByOwnerUserIdOrderByReservationIdDesc(userId, PageRequest.of(0, pageSize + 1))
                    : reservationRepository.findAllByOwnerUserIdAndReservationIdLessThanOrderByReservationIdDesc(
                            userId,
                            cursor,
                            PageRequest.of(0, pageSize + 1)
                    );
        };
        List<MyReservationListResponse> content = reservations.stream()
                .map(MyReservationListResponse::from)
                .toList();
        return CursorPageResponse.of(content, pageSize, MyReservationListResponse::reservationId);
    }

    public MyReservationDetailResponse getMyReservationDetail(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findByReservationIdAndOwnerUserId(reservationId, userId)
                .orElseThrow(() -> new CustomException(ReservationErrorCode.TICKET_NOT_FOUND));
        return MyReservationDetailResponse.from(reservation);
    }
}
