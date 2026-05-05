package com.ddib.monolith.payment.application;

import com.ddib.monolith.payment.api.dto.PaymentPrepareRequest;
import com.ddib.monolith.payment.api.dto.PaymentPrepareResponse;
import com.ddib.monolith.payment.domain.Payment;
import com.ddib.monolith.payment.domain.PaymentStatus;
import com.ddib.monolith.payment.exception.PaymentErrorCode;
import com.ddib.monolith.payment.infra.PaymentRepository;
import com.ddib.monolith.performance.domain.Performance;
import com.ddib.monolith.performance.infra.PerformanceRepository;
import com.ddib.monolith.support.exception.CustomException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentPrepareService {

    private final PaymentRepository paymentRepository;
    private final PerformanceRepository performanceRepository;
    private final OrderIdGenerator orderIdGenerator;
    private final PaymentTokenValidator paymentTokenValidator;
    private final PaymentSeatHoldValidator paymentSeatHoldValidator;
    private final PaymentAmountCalculator paymentAmountCalculator;
    private final Clock clock;

    @Value("${payment.order.currency:KRW}")
    private String currency;

    @Value("${payment.success-url:http://localhost:5173/payment/success}")
    private String successUrl;

    @Value("${payment.fail-url:http://localhost:5173/payment/fail}")
    private String failUrl;

    @Transactional
    public PaymentPrepareResponse prepare(PaymentPrepareRequest request, Long userId) {
        paymentTokenValidator.validateForPrepare(request.performanceId(), request.optionId(), request.tokenId(), userId);
        List<Long> seatIds = paymentAmountCalculator.parseSeatIds(request.seatIds());
        paymentSeatHoldValidator.validate(userId, request.performanceId(), request.optionId(), seatIds);
        int amount = paymentAmountCalculator.calculateAndValidate(request.performanceId(), seatIds, request.amount());

        Payment payment = paymentRepository.findByTokenId(request.tokenId())
                .map(existing -> {
                    if (existing.getStatus() != PaymentStatus.READY) {
                        throw new CustomException(PaymentErrorCode.ORDER_NOT_CONFIRMABLE);
                    }
                    return existing;
                })
                .orElseGet(() -> paymentRepository.save(Payment.createReady(
                        request.tokenId(),
                        orderIdGenerator.generate(),
                        userId,
                        request.performanceId(),
                        request.optionId(),
                        request.seatIds(),
                        amount
                )));

        return buildResponse(payment);
    }

    private PaymentPrepareResponse buildResponse(Payment payment) {
        Performance performance = performanceRepository.findById(payment.getPerformanceId())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.ORDER_NOT_CONFIRMABLE));
        Instant expiresAt = paymentTokenValidator.resolveExpiresAt(
                payment.getPerformanceId(),
                payment.getOptionId(),
                payment.getTokenId(),
                Instant.now(clock)
        );
        return new PaymentPrepareResponse(
                payment.getOrderId(),
                payment.getAmount(),
                currency,
                performance.getTitle() + " " + payment.getSeatIds().size() + "매",
                "user-" + payment.getUserId(),
                successUrl,
                failUrl,
                expiresAt
        );
    }
}
