package com.ddib.monolith.payment.domain.event;

import com.ddib.monolith.payment.domain.Payment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentSucceededEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String traceId,
        String orderId,
        Long paymentId,
        String paymentKey,
        Long userId,
        String tokenId,
        Long performanceId,
        Long optionId,
        List<String> seatIds,
        int amount,
        String method
) {

    public static PaymentSucceededEvent from(Payment payment) {
        return new PaymentSucceededEvent(
                UUID.randomUUID().toString(),
                "PAYMENT_SUCCEEDED",
                Instant.now(),
                null,
                payment.getOrderId(),
                payment.getId(),
                payment.getPaymentKey(),
                payment.getUserId(),
                payment.getTokenId(),
                payment.getPerformanceId(),
                payment.getOptionId(),
                List.copyOf(payment.getSeatIds()),
                payment.getAmount(),
                payment.getMethod()
        );
    }
}
