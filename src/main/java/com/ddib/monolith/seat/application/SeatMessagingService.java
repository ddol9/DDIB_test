package com.ddib.monolith.seat.application;

import com.ddib.monolith.seat.domain.SeatBroadcastMessage;
import com.ddib.monolith.seat.domain.SeatLockResult;
import com.ddib.monolith.seat.domain.SeatMessageType;
import com.ddib.monolith.seat.domain.SeatReleaseResult;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatMessagingService {

    private final BroadcastService broadcastService;

    public void publishLockResult(String sessionId, SeatLockResult result) {
        if (!result.getLockedSeats().isEmpty()) {
            broadcastService.sendToUser(sessionId, SeatBroadcastMessage.builder()
                    .type(SeatMessageType.LOCK_SUCCESS)
                    .performanceId(result.getPerformanceId())
                    .optionId(result.getOptionId())
                    .seatIds(result.getLockedSeats())
                    .expiresAt(result.getExpiresAt())
                    .timestamp(Instant.now().toString())
                    .build());
            broadcastService.broadcastToRoom(result.getPerformanceId(), result.getOptionId(), SeatBroadcastMessage.builder()
                    .type(SeatMessageType.SEAT_LOCKED)
                    .performanceId(result.getPerformanceId())
                    .optionId(result.getOptionId())
                    .seatIds(result.getLockedSeats())
                    .timestamp(Instant.now().toString())
                    .build());
        }
        if (!result.getFailedSeats().isEmpty()) {
            broadcastService.sendToUser(sessionId, SeatBroadcastMessage.builder()
                    .type(SeatMessageType.LOCK_FAILED)
                    .performanceId(result.getPerformanceId())
                    .optionId(result.getOptionId())
                    .seatIds(result.getFailedSeats())
                    .reason("LOCK_FAILED")
                    .timestamp(Instant.now().toString())
                    .build());
        }
    }

    public void publishReleaseResult(SeatReleaseResult result) {
        if (!result.getReleasedSeats().isEmpty()) {
            broadcastService.broadcastToRoom(result.getPerformanceId(), result.getOptionId(), SeatBroadcastMessage.builder()
                    .type(SeatMessageType.SEAT_RELEASED)
                    .performanceId(result.getPerformanceId())
                    .optionId(result.getOptionId())
                    .seatIds(result.getReleasedSeats())
                    .timestamp(Instant.now().toString())
                    .build());
        }
    }
}

