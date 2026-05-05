package com.ddib.monolith.seat.domain;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatReleaseResult {
    private final Long performanceId;
    private final Long optionId;
    private final List<Long> releasedSeats;
}

