package com.ddib.monolith.seat.domain;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatLockResult {
    private final Long performanceId;
    private final Long optionId;
    private final List<Long> lockedSeats;
    private final List<Long> failedSeats;
    private final long expiresAt;
}

