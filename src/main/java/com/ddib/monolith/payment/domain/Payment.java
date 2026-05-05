package com.ddib.monolith.payment.domain;

import com.ddib.monolith.payment.exception.PaymentErrorCode;
import com.ddib.monolith.performance.domain.BaseTimeEntity;
import com.ddib.monolith.support.exception.CommonErrorCode;
import com.ddib.monolith.support.exception.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    private String tokenId;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "performance_id", nullable = false)
    private Long performanceId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Convert(converter = SeatIdsConverter.class)
    @Column(name = "seat_ids", nullable = false, length = 500)
    private List<String> seatIds = new ArrayList<>();

    @Column(name = "amount", nullable = false)
    private int amount;

    @Column(name = "method", length = 20)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private Payment(
            String tokenId,
            String orderId,
            Long userId,
            Long performanceId,
            Long optionId,
            List<String> seatIds,
            int amount,
            String method,
            PaymentStatus status,
            String paymentKey,
            String failReason,
            Instant approvedAt
    ) {
        this.tokenId = tokenId;
        this.orderId = orderId;
        this.userId = userId;
        this.performanceId = performanceId;
        this.optionId = optionId;
        if (seatIds != null) {
            this.seatIds.addAll(seatIds);
        }
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.paymentKey = paymentKey;
        this.failReason = failReason;
        this.approvedAt = approvedAt;
    }

    public static Payment createReady(
            String tokenId,
            String orderId,
            Long userId,
            Long performanceId,
            Long optionId,
            List<String> seatIds,
            int amount
    ) {
        if (tokenId == null || tokenId.isBlank() || seatIds == null || seatIds.isEmpty() || amount <= 0) {
            throw new CustomException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
        return Payment.builder()
                .tokenId(tokenId)
                .orderId(orderId)
                .userId(userId)
                .performanceId(performanceId)
                .optionId(optionId)
                .seatIds(seatIds)
                .amount(amount)
                .status(PaymentStatus.READY)
                .build();
    }

    public void markSuccess(String paymentKey, String method, Instant approvedAt) {
        if (status == PaymentStatus.SUCCESS) {
            return;
        }
        if (status != PaymentStatus.READY) {
            throw new CustomException(PaymentErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        this.paymentKey = paymentKey;
        this.method = method;
        this.approvedAt = approvedAt;
        this.failReason = null;
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed(String reason) {
        this.failReason = reason;
        this.status = PaymentStatus.FAILED;
    }

    public void markInvalidated(String reason) {
        this.failReason = reason;
        this.status = PaymentStatus.INVALIDATED;
    }
}
