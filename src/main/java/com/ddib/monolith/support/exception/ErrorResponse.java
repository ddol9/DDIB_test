package com.ddib.monolith.support.exception;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        String path
) {

    public static ErrorResponse from(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                errorCode.code(),
                errorCode.message(),
                errorCode.status().value(),
                Instant.now(),
                path
        );
    }
}

