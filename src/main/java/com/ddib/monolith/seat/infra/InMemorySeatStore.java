package com.ddib.monolith.seat.infra;

import com.ddib.monolith.seat.domain.SeatStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class InMemorySeatStore implements SeatStore {

    private final Map<String, Long> seatLocks = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> occupiedSeats = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> soldSeats = new ConcurrentHashMap<>();
    private final Set<String> goingToPaymentFlags = ConcurrentHashMap.newKeySet();

    @Override
    public synchronized boolean lockSeat(Long performanceId, Long optionId, Long seatId, Long userId) {
        String soldKey = key(optionId, "sold");
        if (soldSeats.getOrDefault(soldKey, Set.of()).contains(seatId)) {
            return false;
        }
        String lockKey = seatKey(optionId, seatId);
        if (seatLocks.containsKey(lockKey)) {
            return false;
        }
        seatLocks.put(lockKey, userId);
        occupiedSeats.computeIfAbsent(key(optionId, "occupied"), ignored -> new HashSet<>()).add(seatId);
        return true;
    }

    @Override
    public synchronized boolean releaseSeat(Long performanceId, Long optionId, Long seatId, Long userId) {
        String lockKey = seatKey(optionId, seatId);
        Long holder = seatLocks.get(lockKey);
        if (holder == null || !holder.equals(userId)) {
            return false;
        }
        seatLocks.remove(lockKey);
        occupiedSeats.computeIfAbsent(key(optionId, "occupied"), ignored -> new HashSet<>()).remove(seatId);
        return true;
    }

    @Override
    public synchronized Set<Long> getOccupiedSeats(Long performanceId, Long optionId) {
        return Set.copyOf(occupiedSeats.getOrDefault(key(optionId, "occupied"), Set.of()));
    }

    @Override
    public synchronized Set<Long> getSoldSeats(Long performanceId, Long optionId) {
        return Set.copyOf(soldSeats.getOrDefault(key(optionId, "sold"), Set.of()));
    }

    @Override
    public synchronized Set<Long> getMyLockedSeats(Long userId, Long performanceId, Long optionId) {
        Set<Long> results = new HashSet<>();
        for (Long seatId : getOccupiedSeats(performanceId, optionId)) {
            Long holder = seatLocks.get(seatKey(optionId, seatId));
            if (userId.equals(holder)) {
                results.add(seatId);
            }
        }
        return results;
    }

    @Override
    public synchronized Set<Long> releaseAllSeatsForUser(Long userId, Long performanceId, Long optionId) {
        Set<Long> released = new HashSet<>();
        for (Long seatId : new ArrayList<>(getOccupiedSeats(performanceId, optionId))) {
            if (releaseSeat(performanceId, optionId, seatId, userId)) {
                released.add(seatId);
            }
        }
        return released;
    }

    @Override
    public void setGoingToPayment(Long userId, Long performanceId, Long optionId) {
        goingToPaymentFlags.add(flagKey(userId, optionId));
    }

    @Override
    public boolean checkAndClearGoingToPayment(Long userId, Long performanceId, Long optionId) {
        return goingToPaymentFlags.remove(flagKey(userId, optionId));
    }

    @Override
    public synchronized void addSoldSeats(Long performanceId, Long optionId, List<Long> seatIds) {
        soldSeats.computeIfAbsent(key(optionId, "sold"), ignored -> new HashSet<>()).addAll(seatIds);
    }

    private String key(Long optionId, String suffix) {
        return optionId + ":" + suffix;
    }

    private String seatKey(Long optionId, Long seatId) {
        return optionId + ":seat:" + seatId;
    }

    private String flagKey(Long userId, Long optionId) {
        return userId + ":" + optionId + ":going";
    }
}

