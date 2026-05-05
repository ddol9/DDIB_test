package com.ddib.monolith.seat.domain;

public enum SeatMessageType {
    INITIAL_STATE,
    SEAT_LOCKED,
    SEAT_RELEASED,
    SEAT_EXPIRED,
    SEAT_SOLD,
    TOKEN_EXPIRED,
    LOCK_SUCCESS,
    LOCK_FAILED
}

