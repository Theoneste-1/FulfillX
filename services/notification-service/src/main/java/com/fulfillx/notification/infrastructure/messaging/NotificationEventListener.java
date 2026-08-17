package com.fulfillx.notification.infrastructure.messaging;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.idempotency.IdempotentEventProcessor;
import com.fulfillx.notification.application.NotificationDispatchService;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListener {
    private final IdempotentEventProcessor processor;
    private final NotificationDispatchService dispatchService;

    public NotificationEventListener(
            IdempotentEventProcessor processor,
            NotificationDispatchService dispatchService
    ) {
        this.processor = processor;
        this.dispatchService = dispatchService;
    }

    @KafkaListener(topics = {Topics.ORDERS, Topics.PAYMENTS, Topics.INVENTORY, Topics.SHIPMENTS})
    public void onEvent(DomainEvent event) {
        if (event == null || event.eventId() == null) {
            return;
        }
        if (event.correlationId() != null) {
            MDC.put("correlationId", event.correlationId());
        }
        try {
            processor.process(event, dispatchService::dispatch);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
