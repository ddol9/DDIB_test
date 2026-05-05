package com.ddib.monolith.queue.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface QueueStore {

    boolean enqueue(Long performanceId, Long optionId, Long userId);

    void removeFromWaiting(Long performanceId, Long optionId, Long userId);

    long getRank(Long performanceId, Long optionId, Long userId);

    Optional<QueueToken> getUserToken(Long performanceId, Long optionId, Long userId);

    Optional<QueueToken> getToken(Long performanceId, Long optionId, String tokenId);

    QueueToken issueToken(Long performanceId, Long optionId, Long userId, Instant expiresAt);

    List<Long> popWaitingUsers(Long performanceId, Long optionId, long limit);

    long countActiveTokens(Long performanceId, Long optionId, Instant now);

    Set<OptionKey> waitingTargets();

    List<QueueToken> expireTokens(Instant now);

    record OptionKey(Long performanceId, Long optionId) {
    }
}

