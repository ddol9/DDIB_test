package com.ddib.monolith.payment.api;

import com.ddib.monolith.payment.api.dto.PaymentConfirmRequest;
import com.ddib.monolith.payment.api.dto.PaymentConfirmResponse;
import com.ddib.monolith.payment.api.dto.PaymentPrepareRequest;
import com.ddib.monolith.payment.api.dto.PaymentPrepareResponse;
import com.ddib.monolith.payment.api.dto.PaymentStatusResponse;
import com.ddib.monolith.payment.application.PaymentConfirmService;
import com.ddib.monolith.payment.application.PaymentPrepareService;
import com.ddib.monolith.payment.application.PaymentStatusService;
import com.ddib.monolith.support.security.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentPrepareService paymentPrepareService;
    private final PaymentConfirmService paymentConfirmService;
    private final PaymentStatusService paymentStatusService;

    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareResponse> prepare(@UserId Long userId, @Valid @RequestBody PaymentPrepareRequest request) {
        return ResponseEntity.ok(paymentPrepareService.prepare(request, userId));
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirm(@UserId Long userId, @Valid @RequestBody PaymentConfirmRequest request) {
        return ResponseEntity.ok(paymentConfirmService.confirm(request, userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentStatusResponse> getStatus(@UserId Long userId, @PathVariable String orderId) {
        return ResponseEntity.ok(paymentStatusService.getStatus(orderId, userId));
    }
}
