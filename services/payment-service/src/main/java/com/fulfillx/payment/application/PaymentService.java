package com.fulfillx.payment.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.json.Jsons;
import com.fulfillx.common.observability.FulfillxMetrics;
import com.fulfillx.common.security.Roles;
import com.fulfillx.outbox.OutboxWriter;
import com.fulfillx.payment.api.PaymentResponse;
import com.fulfillx.payment.domain.Payment;
import com.fulfillx.payment.domain.PaymentStatus;
import com.fulfillx.payment.infrastructure.persistence.PaymentRepository;
import com.fulfillx.security.FulfillxPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final int FIRST_ATTEMPT = 1;
    private static final long SUCCESS_DELAY_MS = 200L;

    private final PaymentRepository payments;
    private final OutboxWriter outboxWriter;
    private final FulfillxMetrics metrics;

    public PaymentService(PaymentRepository payments, OutboxWriter outboxWriter, FulfillxMetrics metrics) {
        this.payments = payments;
        this.outboxWriter = outboxWriter;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId, FulfillxPrincipal principal) {
        Payment payment = payments.findTopByOrderIdOrderByAttemptDesc(orderId)
                .orElseThrow(() -> new FulfillxException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found for order " + orderId));
        requireRead(payment, principal);
        return PaymentResponse.from(payment);
    }

    @Transactional
    public void onOrderCreated(DomainEvent event) {
        OrderCreatedPayload payload = Jsons.mapper().convertValue(event.payload(), OrderCreatedPayload.class);
        UUID orderId = payload.orderId();
        if (orderId == null) {
            log.warn("OrderCreated missing orderId, event {}", event.eventId());
            return;
        }
        if (payments.existsByOrderIdAndStatus(orderId, PaymentStatus.COMPLETED)) {
            log.info("Skipping OrderCreated; payment already COMPLETED for order {}", orderId);
            return;
        }
        if (payments.existsByOrderIdAndAttempt(orderId, FIRST_ATTEMPT)) {
            log.info("Skipping OrderCreated; payment attempt {} already exists for order {}", FIRST_ATTEMPT, orderId);
            return;
        }

        BigDecimal amount = payload.totalAmount() == null ? BigDecimal.ZERO : payload.totalAmount();
        String currency = payload.currency() == null ? "USD" : payload.currency();
        UUID customerId = payload.customerId() == null ? orderId : payload.customerId();
        Payment payment = Payment.pending(orderId, customerId, amount, currency, FIRST_ATTEMPT);
        try {
            payments.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            log.info("Duplicate payment insert for order {}", orderId);
            return;
        }

        boolean paymentFail = payload.simulation() != null && Boolean.TRUE.equals(payload.simulation().paymentFail());
        String causation = event.eventId().toString();
        if (PaymentSimulator.shouldFail(paymentFail, amount)) {
            String reason = PaymentSimulator.failureReason(paymentFail, amount);
            payment.fail(reason);
            metrics.increment("fulfillx.payments.failures");
            outboxWriter.enqueue(
                    EventTypes.PAYMENT_FAILED,
                    "Payment",
                    payment.getId().toString(),
                    failedPayload(payment, reason),
                    causation
            );
            return;
        }

        sleepQuietly();
        String reference = "pay_sandbox_" + payment.getId().toString().replace("-", "");
        payment.complete(reference);
        outboxWriter.enqueue(
                EventTypes.PAYMENT_COMPLETED,
                "Payment",
                payment.getId().toString(),
                completedPayload(payment),
                causation
        );
    }

    @Transactional
    public void onRefundRequested(DomainEvent event) {
        RefundRequestedPayload payload = Jsons.mapper().convertValue(event.payload(), RefundRequestedPayload.class);
        UUID orderId = payload.orderId();
        Payment payment = payload.paymentId() != null
                ? payments.findById(payload.paymentId()).orElse(null)
                : null;
        if (payment == null) {
            payment = payments.findFirstByOrderIdAndStatusOrderByAttemptDesc(orderId, PaymentStatus.COMPLETED)
                    .orElse(null);
        }
        if (payment == null) {
            log.info("No COMPLETED payment to refund for order {}", orderId);
            return;
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.info("Skipping refund for payment {} in status {}", payment.getId(), payment.getStatus());
            return;
        }
        payment.refund();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("paymentId", payment.getId());
        body.put("orderId", payment.getOrderId());
        body.put("amount", payment.getAmount());
        body.put("currency", payment.getCurrency());
        outboxWriter.enqueue(
                EventTypes.PAYMENT_REFUNDED,
                "Payment",
                payment.getId().toString(),
                body,
                event.eventId().toString()
        );
    }

    private void requireRead(Payment payment, FulfillxPrincipal principal) {
        boolean privileged = principal.roles() != null
                && (principal.roles().contains(Roles.SUPPORT) || principal.roles().contains(Roles.ADMIN));
        if (!privileged && !payment.getCustomerId().equals(principal.userId())) {
            throw new AccessDeniedException("Not allowed to access this payment");
        }
    }

    private Map<String, Object> completedPayload(Payment payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("orderId", payment.getOrderId());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("provider", payment.getProvider());
        payload.put("providerReference", payment.getProviderReference());
        return payload;
    }

    private Map<String, Object> failedPayload(Payment payment, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("orderId", payment.getOrderId());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("reason", reason);
        return payload;
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(SUCCESS_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderCreatedPayload(
            UUID orderId,
            UUID customerId,
            BigDecimal totalAmount,
            String currency,
            SimulationPayload simulation
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SimulationPayload(Boolean paymentFail) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundRequestedPayload(UUID orderId, UUID paymentId, BigDecimal amount, String currency, String reason) {
    }
}
