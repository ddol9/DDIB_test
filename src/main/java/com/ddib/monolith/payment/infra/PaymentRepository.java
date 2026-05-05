package com.ddib.monolith.payment.infra;

import com.ddib.monolith.payment.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTokenId(String tokenId);

    Optional<Payment> findByOrderId(String orderId);
}
