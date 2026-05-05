package com.ddib.monolith.performance.api.dto;

import java.util.List;

public record SeatTypeInfo(String seatType, int price, List<String> rows, int seatsPerRow) {
}

