package com.fulfillx.payment.api;

import com.fulfillx.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        String status,
        String provider,
        String providerReference,
        String failureReason,
        int attempt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getProvider(),
                payment.getProviderReference(),
                payment.getFailureReason(),
                payment.getAttempt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
