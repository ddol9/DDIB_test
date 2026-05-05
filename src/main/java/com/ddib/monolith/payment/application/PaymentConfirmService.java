package com.ddib.monolith.payment.application;

import com.ddib.monolith.payment.api.dto.PaymentConfirmRequest;
import com.ddib.monolith.payment.api.dto.PaymentConfirmResponse;
import com.ddib.monolith.payment.domain.Payment;
import com.ddib.monolith.payment.domain.PaymentStatus;
import com.ddib.monolith.payment.domain.event.PaymentSucceededEvent;
import com.ddib.monolith.payment.exception.PaymentErrorCode;
import com.ddib.monolith.payment.infra.PaymentRepository;
import com.ddib.monolith.payment.pg.PaymentPgClient;
import com.ddib.monolith.payment.pg.PgConfirmResult;
import com.ddib.monolith.support.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentConfirmService {

    private final PaymentRepository paymentRepository;
    private final PaymentTokenValidator paymentTokenValidator;
    private final PaymentPgClient paymentPgClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(noRollbackFor = CustomException.class)
    public PaymentConfirmResponse confirm(PaymentConfirmRequest request, Long userId) {
        Payment payment = paymentRepository.findByOrderId(request.orderId())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.ORDER_NOT_FOUND));
        if (!payment.getUserId().equals(userId)) {
            throw new CustomException(PaymentErrorCode.TOKEN_MISMATCH);
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentConfirmResponse.from(payment, true);
        }
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new CustomException(PaymentErrorCode.ORDER_NOT_CONFIRMABLE);
        }
        if (request.amount() == null || payment.getAmount() != request.amount()) {
            payment.markInvalidated("INVALID_AMOUNT");
            paymentRepository.save(payment);
            throw new CustomException(PaymentErrorCode.INVALID_AMOUNT);
        }

        paymentTokenValidator.validateForPayment(payment);
        PgConfirmResult confirmResult = paymentPgClient.confirm(request.paymentKey(), payment.getOrderId(), payment.getAmount());
        if (!confirmResult.success()) {
            payment.markFailed(confirmResult.errorMessage());
            paymentRepository.save(payment);
            throw new CustomException(PaymentErrorCode.PG_ERROR);
        }

        payment.markSuccess(request.paymentKey(), confirmResult.method(), confirmResult.approvedAt());
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishEvent(PaymentSucceededEvent.from(saved));
        return PaymentConfirmResponse.from(saved, false);
    }
}
