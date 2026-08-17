package com.fulfillx.payment.infrastructure.persistence;

import com.fulfillx.payment.domain.Payment;
import com.fulfillx.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    boolean existsByOrderIdAndAttempt(UUID orderId, int attempt);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByAttemptDesc(UUID orderId, PaymentStatus status);

    Optional<Payment> findTopByOrderIdOrderByAttemptDesc(UUID orderId);
}
