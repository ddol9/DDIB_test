package com.ddib.monolith.seat.domain;

import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InitialStateMessage {
    private SeatMessageType type;
    private Long performanceId;
    private Long optionId;
    private Set<Long> occupiedSeats;
    private Set<Long> soldSeats;
    private Set<Long> myLockedSeats;
    private Long expiresAt;
    private String timestamp;
}

