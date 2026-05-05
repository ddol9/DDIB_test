package com.ddib.monolith.payment.exception;

import com.ddib.monolith.support.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PaymentErrorCode implements ErrorCode {
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "PAYMENT_400_AMOUNT", "Payment amount is invalid."),
    TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "PAYMENT_401_TOKEN", "Queue token does not match the request."),
    TOKEN_EXPIRED(HttpStatus.GONE, "PAYMENT_410_TOKEN", "Queue token has expired."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_404_ORDER", "Payment order was not found."),
    ORDER_NOT_CONFIRMABLE(HttpStatus.CONFLICT, "PAYMENT_409_CONFIRM", "Payment cannot be confirmed in the current state."),
    SEAT_NOT_HELD(HttpStatus.CONFLICT, "PAYMENT_409_SEAT", "Requested seats are not currently held by the user."),
    PG_ERROR(HttpStatus.BAD_GATEWAY, "PAYMENT_502_PG", "Payment gateway confirmation failed.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PaymentErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
