package com.ddib.monolith.payment.application;

import com.ddib.monolith.payment.domain.Payment;
import com.ddib.monolith.payment.exception.PaymentErrorCode;
import com.ddib.monolith.queue.application.QueueService;
import com.ddib.monolith.queue.domain.QueueToken;
import com.ddib.monolith.support.exception.CustomException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentTokenValidator {

    private final QueueService queueService;

    public QueueToken validateForPrepare(Long performanceId, Long optionId, String tokenId, Long userId) {
        QueueToken token = queueService.findValidToken(performanceId, optionId, tokenId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.TOKEN_EXPIRED));
        if (!token.userId().equals(userId)) {
            throw new CustomException(PaymentErrorCode.TOKEN_MISMATCH);
        }
        return token;
    }

    public QueueToken validateForPayment(Payment payment) {
        QueueToken token = queueService.findValidToken(payment.getPerformanceId(), payment.getOptionId(), payment.getTokenId())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.TOKEN_EXPIRED));
        if (!token.userId().equals(payment.getUserId())) {
            throw new CustomException(PaymentErrorCode.TOKEN_MISMATCH);
        }
        return token;
    }

    public Instant resolveExpiresAt(Long performanceId, Long optionId, String tokenId, Instant now) {
        long ttl = queueService.getTokenTtlSeconds(performanceId, optionId, tokenId);
        if (ttl <= 0) {
            return null;
        }
        return now.plusSeconds(ttl);
    }
}
