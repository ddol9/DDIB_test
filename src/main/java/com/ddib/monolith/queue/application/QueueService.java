package com.ddib.monolith.queue.application;

import com.ddib.monolith.queue.domain.QueueConstants;
import com.ddib.monolith.queue.domain.QueueRequestStatus;
import com.ddib.monolith.queue.domain.QueueStatusResponse;
import com.ddib.monolith.queue.domain.QueueStore;
import com.ddib.monolith.queue.domain.QueueToken;
import com.ddib.monolith.seat.application.SeatExpirationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueueService {

    private final QueueStore queueStore;
    private final QueueEmitterRegistry emitterRegistry;
    private final ObjectProvider<SeatExpirationService> seatExpirationServiceProvider;

    @Transactional
    public boolean registerWaitQueue(Long performanceId, Long optionId, Long userId) {
        queueStore.getUserToken(performanceId, optionId, userId)
                .filter(token -> !token.isExpired(Instant.now()))
                .ifPresentOrElse(
                        token -> {
                        },
                        () -> queueStore.enqueue(performanceId, optionId, userId)
                );
        tryIssueTokens(performanceId, optionId);
        return true;
    }

    public QueueStatusResponse getStatus(Long performanceId, Long optionId, Long userId) {
        Optional<QueueToken> issued = queueStore.getUserToken(performanceId, optionId, userId)
                .filter(token -> !token.isExpired(Instant.now()));
        if (issued.isPresent()) {
            return QueueStatusResponse.builder()
                    .rank(0L)
                    .queueToken(issued.get().tokenId())
                    .status(QueueRequestStatus.ISSUED)
                    .build();
        }

        long rank = queueStore.getRank(performanceId, optionId, userId);
        if (rank >= 0) {
            return QueueStatusResponse.builder()
                    .rank(rank + 1)
                    .status(QueueRequestStatus.WAITING)
                    .build();
        }

        return QueueStatusResponse.builder()
                .rank(-1L)
                .status(QueueRequestStatus.REJECT)
                .build();
    }

    public QueueStatusResponse registerEmitterAndGetCurrentStatus(Long performanceId, Long optionId, Long userId) {
        return getStatus(performanceId, optionId, userId);
    }

    public Optional<QueueToken> findValidToken(Long performanceId, Long optionId, String tokenId) {
        return queueStore.getToken(performanceId, optionId, tokenId)
                .filter(token -> !token.isExpired(Instant.now()));
    }

    public long getTokenTtlSeconds(Long performanceId, Long optionId, String tokenId) {
        return findValidToken(performanceId, optionId, tokenId)
                .map(token -> ChronoUnit.SECONDS.between(Instant.now(), token.expiresAt()))
                .filter(ttl -> ttl > 0)
                .orElse(-1L);
    }

    @Transactional
    public void schedule() {
        for (QueueStore.OptionKey target : queueStore.waitingTargets()) {
            tryIssueTokens(target.performanceId(), target.optionId());
        }
        List<QueueToken> expired = queueStore.expireTokens(Instant.now());
        if (!expired.isEmpty()) {
            seatExpirationServiceProvider.ifAvailable(service -> expired.forEach(service::expireToken));
        }
    }

    @Transactional
    public void tryIssueTokens(Long performanceId, Long optionId) {
        Instant now = Instant.now();
        long activeUsers = queueStore.countActiveTokens(performanceId, optionId, now);
        long availableSlots = QueueConstants.MAX_ACTIVE_USERS - activeUsers;
        if (availableSlots <= 0) {
            return;
        }
        long countToIssue = Math.min(availableSlots, QueueConstants.MAX_ENTRIES_PER_CYCLE);
        List<Long> issuedUsers = queueStore.popWaitingUsers(performanceId, optionId, countToIssue);
        for (Long userId : issuedUsers) {
            QueueToken token = queueStore.issueToken(
                    performanceId,
                    optionId,
                    userId,
                    now.plus(QueueConstants.ACTIVE_USER_TIMEOUT_MINUTES, ChronoUnit.MINUTES)
            );
            emitterRegistry.push(
                    performanceId,
                    optionId,
                    userId,
                    QueueStatusResponse.builder()
                            .rank(0L)
                            .queueToken(token.tokenId())
                            .status(QueueRequestStatus.ISSUED)
                            .build(),
                    true
            );
        }
    }
}
