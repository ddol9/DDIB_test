package com.ddib.monolith.payment.application;

import com.ddib.monolith.payment.exception.PaymentErrorCode;
import com.ddib.monolith.performance.domain.Seat;
import com.ddib.monolith.performance.domain.SeatTypePrice;
import com.ddib.monolith.performance.infra.SeatRepository;
import com.ddib.monolith.performance.infra.SeatTypePriceRepository;
import com.ddib.monolith.support.exception.CustomException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentAmountCalculator {

    private final SeatRepository seatRepository;
    private final SeatTypePriceRepository seatTypePriceRepository;

    public List<Long> parseSeatIds(List<String> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new CustomException(PaymentErrorCode.SEAT_NOT_HELD);
        }
        List<Long> parsed = new ArrayList<>();
        for (String seatId : new LinkedHashSet<>(seatIds)) {
            try {
                parsed.add(Long.parseLong(seatId));
            } catch (NumberFormatException exception) {
                throw new CustomException(PaymentErrorCode.SEAT_NOT_HELD);
            }
        }
        return parsed;
    }

    public int calculateAndValidate(Long performanceId, List<Long> seatIds, Integer requestedAmount) {
        int calculatedAmount = 0;
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new CustomException(PaymentErrorCode.SEAT_NOT_HELD));
            SeatTypePrice seatTypePrice = seatTypePriceRepository.findByPerformanceIdAndSeatLabel(performanceId, seat.getSeatLabel())
                    .orElseThrow(() -> new CustomException(PaymentErrorCode.INVALID_AMOUNT));
            calculatedAmount += seatTypePrice.getPrice();
        }
        if (requestedAmount == null || calculatedAmount != requestedAmount) {
            throw new CustomException(PaymentErrorCode.INVALID_AMOUNT);
        }
        return calculatedAmount;
    }
}
