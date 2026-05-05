package com.ddib.monolith.queue.infra;

import com.ddib.monolith.queue.domain.QueueStore;
import com.ddib.monolith.queue.domain.QueueToken;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class InMemoryQueueStore implements QueueStore {

    private final Map<OptionKey, LinkedHashSet<Long>> waitingUsers = new ConcurrentHashMap<>();
    private final Map<OptionKey, Map<Long, QueueToken>> userTokens = new ConcurrentHashMap<>();
    private final Map<OptionKey, Map<String, QueueToken>> activeTokens = new ConcurrentHashMap<>();

    @Override
    public synchronized boolean enqueue(Long performanceId, Long optionId, Long userId) {
        return waitingUsers
                .computeIfAbsent(new OptionKey(performanceId, optionId), key -> new LinkedHashSet<>())
                .add(userId);
    }

    @Override
    public synchronized void removeFromWaiting(Long performanceId, Long optionId, Long userId) {
        LinkedHashSet<Long> waiting = waitingUsers.get(new OptionKey(performanceId, optionId));
        if (waiting != null) {
            waiting.remove(userId);
        }
    }

    @Override
    public synchronized long getRank(Long performanceId, Long optionId, Long userId) {
        LinkedHashSet<Long> waiting = waitingUsers.get(new OptionKey(performanceId, optionId));
        if (waiting == null) {
            return -1L;
        }
        int index = 0;
        for (Long current : waiting) {
            if (current.equals(userId)) {
                return index;
            }
            index++;
        }
        return -1L;
    }

    @Override
    public synchronized Optional<QueueToken> getUserToken(Long performanceId, Long optionId, Long userId) {
        Map<Long, QueueToken> tokens = userTokens.get(new OptionKey(performanceId, optionId));
        return tokens == null ? Optional.empty() : Optional.ofNullable(tokens.get(userId));
    }

    @Override
    public synchronized Optional<QueueToken> getToken(Long performanceId, Long optionId, String tokenId) {
        Map<String, QueueToken> tokens = activeTokens.get(new OptionKey(performanceId, optionId));
        return tokens == null ? Optional.empty() : Optional.ofNullable(tokens.get(tokenId));
    }

    @Override
    public synchronized QueueToken issueToken(Long performanceId, Long optionId, Long userId, Instant expiresAt) {
        String tokenId = UUID.randomUUID().toString();
        QueueToken queueToken = new QueueToken(performanceId, optionId, userId, tokenId, expiresAt);
        OptionKey key = new OptionKey(performanceId, optionId);
        userTokens.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put(userId, queueToken);
        activeTokens.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put(tokenId, queueToken);
        removeFromWaiting(performanceId, optionId, userId);
        return queueToken;
    }

    @Override
    public synchronized List<Long> popWaitingUsers(Long performanceId, Long optionId, long limit) {
        LinkedHashSet<Long> waiting = waitingUsers.get(new OptionKey(performanceId, optionId));
        if (waiting == null || waiting.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<Long> issued = new ArrayList<>();
        Iterator<Long> iterator = waiting.iterator();
        while (iterator.hasNext() && issued.size() < limit) {
            issued.add(iterator.next());
            iterator.remove();
        }
        return issued;
    }

    @Override
    public synchronized long countActiveTokens(Long performanceId, Long optionId, Instant now) {
        cleanupExpired(new OptionKey(performanceId, optionId), now);
        Map<String, QueueToken> tokens = activeTokens.get(new OptionKey(performanceId, optionId));
        return tokens == null ? 0L : tokens.size();
    }

    @Override
    public synchronized Set<OptionKey> waitingTargets() {
        return Set.copyOf(waitingUsers.keySet());
    }

    @Override
    public synchronized List<QueueToken> expireTokens(Instant now) {
        List<QueueToken> expired = new ArrayList<>();
        for (OptionKey key : new ArrayList<>(activeTokens.keySet())) {
            Map<String, QueueToken> byToken = activeTokens.get(key);
            if (byToken == null) {
                continue;
            }
            Iterator<Map.Entry<String, QueueToken>> iterator = byToken.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, QueueToken> entry = iterator.next();
                QueueToken token = entry.getValue();
                if (token.isExpired(now)) {
                    iterator.remove();
                    Map<Long, QueueToken> byUser = userTokens.get(key);
                    if (byUser != null) {
                        byUser.remove(token.userId());
                    }
                    expired.add(token);
                }
            }
        }
        return expired;
    }

    private void cleanupExpired(OptionKey key, Instant now) {
        Map<String, QueueToken> byToken = activeTokens.get(key);
        if (byToken == null) {
            return;
        }
        Iterator<Map.Entry<String, QueueToken>> iterator = byToken.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, QueueToken> entry = iterator.next();
            QueueToken token = entry.getValue();
            if (token.isExpired(now)) {
                iterator.remove();
                Map<Long, QueueToken> byUser = userTokens.get(key);
                if (byUser != null) {
                    byUser.remove(token.userId());
                }
            }
        }
    }
}

