package com.ddib.monolith.seat.application;

import com.ddib.monolith.queue.application.QueueService;
import com.ddib.monolith.queue.domain.QueueToken;
import com.ddib.monolith.seat.domain.InitialStateMessage;
import com.ddib.monolith.seat.domain.SeatLockResult;
import com.ddib.monolith.seat.domain.SeatMessageType;
import com.ddib.monolith.seat.domain.SeatReleaseResult;
import com.ddib.monolith.seat.domain.SeatStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatLockService implements SeatExpirationService {

    private final SeatStore seatStore;
    private final QueueService queueService;
    private final BroadcastService broadcastService;

    public InitialStateMessage getInitialState(Long userId, Long performanceId, Long optionId, String tokenId) {
        long ttl = queueService.getTokenTtlSeconds(performanceId, optionId, tokenId);
        Long expiresAt = ttl > 0 ? Instant.now().plusSeconds(ttl).getEpochSecond() : null;
        return InitialStateMessage.builder()
                .type(SeatMessageType.INITIAL_STATE)
                .performanceId(performanceId)
                .optionId(optionId)
                .occupiedSeats(seatStore.getOccupiedSeats(performanceId, optionId))
                .soldSeats(seatStore.getSoldSeats(performanceId, optionId))
                .myLockedSeats(seatStore.getMyLockedSeats(userId, performanceId, optionId))
                .expiresAt(expiresAt)
                .timestamp(Instant.now().toString())
                .build();
    }

    public SeatLockResult lockSeats(Long userId, Long performanceId, Long optionId, String tokenId, List<Long> seatIds) {
        long ttl = queueService.getTokenTtlSeconds(performanceId, optionId, tokenId);
        if (ttl <= 0) {
            return SeatLockResult.builder()
                    .performanceId(performanceId)
                    .optionId(optionId)
                    .lockedSeats(List.of())
                    .failedSeats(seatIds)
                    .expiresAt(Instant.now().getEpochSecond())
                    .build();
        }
        List<Long> locked = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        for (Long seatId : seatIds) {
            if (seatStore.lockSeat(performanceId, optionId, seatId, userId)) {
                locked.add(seatId);
            } else {
                failed.add(seatId);
            }
        }
        return SeatLockResult.builder()
                .performanceId(performanceId)
                .optionId(optionId)
                .lockedSeats(locked)
                .failedSeats(failed)
                .expiresAt(Instant.now().plusSeconds(ttl).getEpochSecond())
                .build();
    }

    public SeatReleaseResult releaseSeats(Long userId, Long performanceId, Long optionId, List<Long> seatIds) {
        List<Long> released = new ArrayList<>();
        for (Long seatId : seatIds) {
            if (seatStore.releaseSeat(performanceId, optionId, seatId, userId)) {
                released.add(seatId);
            }
        }
        return SeatReleaseResult.builder()
                .performanceId(performanceId)
                .optionId(optionId)
                .releasedSeats(released)
                .build();
    }

    public void setGoingToPayment(Long userId, Long performanceId, Long optionId) {
        seatStore.setGoingToPayment(userId, performanceId, optionId);
    }

    @Override
    public void expireToken(QueueToken queueToken) {
        Set<Long> released = seatStore.releaseAllSeatsForUser(queueToken.userId(), queueToken.performanceId(), queueToken.optionId());
        if (!released.isEmpty()) {
            broadcastService.broadcastToRoom(
                    queueToken.performanceId(),
                    queueToken.optionId(),
                    com.ddib.monolith.seat.domain.SeatBroadcastMessage.builder()
                            .type(SeatMessageType.SEAT_EXPIRED)
                            .performanceId(queueToken.performanceId())
                            .optionId(queueToken.optionId())
                            .seatIds(List.copyOf(released))
                            .timestamp(Instant.now().toString())
                            .build()
            );
        }
    }
}

