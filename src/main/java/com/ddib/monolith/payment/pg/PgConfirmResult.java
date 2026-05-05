package com.ddib.monolith.payment.pg;

import java.time.Instant;

public record PgConfirmResult(
        boolean success,
        String method,
        Instant approvedAt,
        String errorMessage
) {

    public static PgConfirmResult success(String method, Instant approvedAt) {
        return new PgConfirmResult(true, method, approvedAt, null);
    }

    public static PgConfirmResult failure(String errorMessage) {
        return new PgConfirmResult(false, null, null, errorMessage);
    }
}
