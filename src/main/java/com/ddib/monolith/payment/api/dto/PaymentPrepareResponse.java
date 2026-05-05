package com.ddib.monolith.payment.api.dto;

import java.time.Instant;

public record PaymentPrepareResponse(
        String orderId,
        int amount,
        String currency,
        String orderName,
        String customerKey,
        String successUrl,
        String failUrl,
        Instant expiresAt
) {
}
