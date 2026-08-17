package com.fulfillx.payment.infrastructure.messaging;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.idempotency.IdempotentEventProcessor;
import com.fulfillx.payment.application.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final PaymentService paymentService;

    public OrderEventConsumer(IdempotentEventProcessor idempotentEventProcessor, PaymentService paymentService) {
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = Topics.ORDERS, groupId = "payment-service")
    public void onOrderEvent(DomainEvent event) {
        idempotentEventProcessor.process(event, this::handle);
    }

    private void handle(DomainEvent event) {
        switch (event.eventType()) {
            case EventTypes.ORDER_CREATED -> paymentService.onOrderCreated(event);
            case EventTypes.REFUND_REQUESTED -> paymentService.onRefundRequested(event);
            default -> {
            }
        }
    }
}
