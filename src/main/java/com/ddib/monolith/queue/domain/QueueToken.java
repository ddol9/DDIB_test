package com.ddib.monolith.queue.domain;

import java.time.Instant;

public record QueueToken(
        Long performanceId,
        Long optionId,
        Long userId,
        String tokenId,
        Instant expiresAt
) {

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }
}

