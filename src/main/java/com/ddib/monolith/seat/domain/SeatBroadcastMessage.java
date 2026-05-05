package com.ddib.monolith.seat.domain;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatBroadcastMessage {
    private SeatMessageType type;
    private Long performanceId;
    private Long optionId;
    private Long userId;
    private List<Long> seatIds;
    private Long expiresAt;
    private String timestamp;
    private String reason;
}

