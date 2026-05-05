package com.ddib.monolith.payment.application;

import com.ddib.monolith.payment.api.dto.PaymentStatusResponse;
import com.ddib.monolith.payment.domain.Payment;
import com.ddib.monolith.payment.domain.PaymentStatus;
import com.ddib.monolith.payment.exception.PaymentErrorCode;
import com.ddib.monolith.payment.infra.PaymentRepository;
import com.ddib.monolith.performance.domain.Performance;
import com.ddib.monolith.performance.domain.PerformanceOption;
import com.ddib.monolith.performance.domain.Seat;
import com.ddib.monolith.performance.infra.PerformanceOptionRepository;
import com.ddib.monolith.performance.infra.PerformanceRepository;
import com.ddib.monolith.performance.infra.SeatRepository;
import com.ddib.monolith.support.exception.CustomException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentStatusService {

    private final PaymentRepository paymentRepository;
    private final PaymentTokenValidator paymentTokenValidator;
    private final PerformanceRepository performanceRepository;
    private final PerformanceOptionRepository performanceOptionRepository;
    private final SeatRepository seatRepository;
    private final Clock clock;

    public PaymentStatusResponse getStatus(String orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.ORDER_NOT_FOUND));
        if (!payment.getUserId().equals(userId)) {
            throw new CustomException(PaymentErrorCode.TOKEN_MISMATCH);
        }

        Performance performance = performanceRepository.findById(payment.getPerformanceId()).orElse(null);
        PerformanceOption option = performanceOptionRepository.findById(payment.getOptionId()).orElse(null);
        List<String> seatLabels = resolveSeatLabels(payment.getSeatIds());
        Instant expiresAt = payment.getStatus() == PaymentStatus.READY
                ? paymentTokenValidator.resolveExpiresAt(payment.getPerformanceId(), payment.getOptionId(), payment.getTokenId(), Instant.now(clock))
                : null;
        LocalDateTime lastUpdatedAt = payment.getUpdatedAt();

        return new PaymentStatusResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getPerformanceId(),
                payment.getOptionId(),
                List.copyOf(payment.getSeatIds()),
                seatLabels,
                performance != null ? performance.getTitle() : null,
                performance != null ? performance.getPerformanceImg() : null,
                performance != null && performance.getVenue() != null ? performance.getVenue().getName() : null,
                option != null ? option.getStartAt() : null,
                payment.getApprovedAt(),
                expiresAt,
                lastUpdatedAt
        );
    }

    private List<String> resolveSeatLabels(List<String> seatIds) {
        List<Long> parsedSeatIds = new ArrayList<>();
        for (String seatId : seatIds) {
            try {
                parsedSeatIds.add(Long.parseLong(seatId));
            } catch (NumberFormatException ignored) {
                return List.of();
            }
        }
        Map<Long, Seat> seatMap = new HashMap<>();
        for (Seat seat : seatRepository.findAllById(parsedSeatIds)) {
            seatMap.put(seat.getId(), seat);
        }
        List<String> labels = new ArrayList<>();
        for (Long seatId : parsedSeatIds) {
            Seat seat = seatMap.get(seatId);
            if (seat != null) {
                labels.add(seat.getSeatLabel() + "-" + seat.getSeatNumber());
            }
        }
        return labels;
    }
}
