package com.ddib.monolith.payment.application;

import com.ddib.monolith.payment.exception.PaymentErrorCode;
import com.ddib.monolith.seat.domain.SeatStore;
import com.ddib.monolith.support.exception.CustomException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSeatHoldValidator {

    private final SeatStore seatStore;

    public void validate(Long userId, Long performanceId, Long optionId, List<Long> requestedSeatIds) {
        Set<Long> heldSeats = seatStore.getMyLockedSeats(userId, performanceId, optionId);
        if (!heldSeats.containsAll(new HashSet<>(requestedSeatIds))) {
            throw new CustomException(PaymentErrorCode.SEAT_NOT_HELD);
        }
    }
}
