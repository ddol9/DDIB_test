package com.ddib.monolith.performance.api.dto;

import java.util.List;

public record SeatRedisInfo(List<SeatTypeInfo> seatConfiguration, List<SeatInfo> seats) {
}

