package com.ddib.monolith.payment.pg;

public interface PaymentPgClient {

    PgConfirmResult confirm(String paymentKey, String orderId, int amount);
}
