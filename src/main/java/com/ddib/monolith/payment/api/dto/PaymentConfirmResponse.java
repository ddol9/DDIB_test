package com.ddib.monolith.payment.api.dto;

import com.ddib.monolith.payment.domain.Payment;
import java.time.Instant;

public record PaymentConfirmResponse(
        Long paymentId,
        String orderId,
        String status,
        Instant approvedAt,
        boolean idempotent
) {

    public static PaymentConfirmResponse from(Payment payment, boolean idempotent) {
        return new PaymentConfirmResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus().name(),
                payment.getApprovedAt(),
                idempotent
        );
    }
}
