package com.ddib.monolith.payment.pg;

import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StubPaymentPgClient implements PaymentPgClient {

    private final Clock clock;

    @Override
    public PgConfirmResult confirm(String paymentKey, String orderId, int amount) {
        if (paymentKey == null || paymentKey.isBlank() || orderId == null || orderId.isBlank() || amount <= 0) {
            return PgConfirmResult.failure("Invalid PG confirmation request.");
        }
        return PgConfirmResult.success("CARD", Instant.now(clock));
    }
}
