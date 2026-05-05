package com.ddib.monolith.seat.domain;

import java.util.List;
import java.util.Set;

public interface SeatStore {

    boolean lockSeat(Long performanceId, Long optionId, Long seatId, Long userId);

    boolean releaseSeat(Long performanceId, Long optionId, Long seatId, Long userId);

    Set<Long> getOccupiedSeats(Long performanceId, Long optionId);

    Set<Long> getSoldSeats(Long performanceId, Long optionId);

    Set<Long> getMyLockedSeats(Long userId, Long performanceId, Long optionId);

    Set<Long> releaseAllSeatsForUser(Long userId, Long performanceId, Long optionId);

    void setGoingToPayment(Long userId, Long performanceId, Long optionId);

    boolean checkAndClearGoingToPayment(Long userId, Long performanceId, Long optionId);

    void addSoldSeats(Long performanceId, Long optionId, List<Long> seatIds);
}

