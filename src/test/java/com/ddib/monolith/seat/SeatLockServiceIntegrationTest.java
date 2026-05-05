package com.ddib.monolith.seat;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddib.monolith.queue.application.QueueService;
import com.ddib.monolith.queue.domain.QueueToken;
import com.ddib.monolith.seat.application.SeatLockService;
import com.ddib.monolith.seat.domain.InitialStateMessage;
import com.ddib.monolith.seat.domain.SeatLockResult;
import com.ddib.monolith.seat.domain.SeatReleaseResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SeatLockServiceIntegrationTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private SeatLockService seatLockService;

    @Test
    void shouldLockAndReleaseSeatsWithIssuedQueueToken() {
        queueService.registerWaitQueue(1L, 1L, 1L);
        QueueToken queueToken = queueService.findValidToken(1L, 1L, queueService.getStatus(1L, 1L, 1L).getQueueToken()).orElseThrow();

        InitialStateMessage initialState = seatLockService.getInitialState(1L, 1L, 1L, queueToken.tokenId());
        assertThat(initialState.getOccupiedSeats()).isEmpty();

        SeatLockResult lockResult = seatLockService.lockSeats(1L, 1L, 1L, queueToken.tokenId(), List.of(1L, 2L));
        assertThat(lockResult.getLockedSeats()).containsExactlyInAnyOrder(1L, 2L);

        SeatReleaseResult releaseResult = seatLockService.releaseSeats(1L, 1L, 1L, List.of(1L));
        assertThat(releaseResult.getReleasedSeats()).containsExactly(1L);
    }
}
