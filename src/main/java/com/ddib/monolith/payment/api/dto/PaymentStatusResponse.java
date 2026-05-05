package com.ddib.monolith.payment.api.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentStatusResponse(
        Long paymentId,
        String orderId,
        String status,
        int amount,
        Long performanceId,
        Long optionId,
        List<String> seatIds,
        List<String> seatLabels,
        String performanceTitle,
        String performanceImg,
        String venueName,
        LocalDateTime performanceStartAt,
        Instant approvedAt,
        Instant expiresAt,
        LocalDateTime lastUpdatedAt
) {
}
