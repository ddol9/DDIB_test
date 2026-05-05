package com.ddib.monolith.seat.domain;

import java.util.List;

public record SeatLockRequest(
        Long performanceId,
        Long optionId,
        List<Long> seatIds
) {
}

