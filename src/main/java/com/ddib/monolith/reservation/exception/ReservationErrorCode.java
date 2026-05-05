package com.ddib.monolith.reservation.exception;

import com.ddib.monolith.support.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ReservationErrorCode implements ErrorCode {
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "RESERVATION_404_TICKET", "Reservation was not found.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ReservationErrorCode(HttpStatus status, String code, String message) {
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
