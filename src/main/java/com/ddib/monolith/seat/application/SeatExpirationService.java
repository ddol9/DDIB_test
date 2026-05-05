package com.ddib.monolith.seat.application;

import com.ddib.monolith.queue.domain.QueueToken;

public interface SeatExpirationService {

    void expireToken(QueueToken queueToken);
}

