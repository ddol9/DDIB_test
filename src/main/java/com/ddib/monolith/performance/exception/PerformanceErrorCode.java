package com.ddib.monolith.performance.exception;

import com.ddib.monolith.support.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum PerformanceErrorCode implements ErrorCode {
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PERFORMANCE_404", "Performance not found.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    PerformanceErrorCode(HttpStatus status, String code, String message) {
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

