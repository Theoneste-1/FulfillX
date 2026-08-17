package com.fulfillx.analytics.infrastructure.messaging;

import com.fulfillx.analytics.application.AnalyticsIngestService;
import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.idempotency.IdempotentEventProcessor;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsEventListener {
    private final IdempotentEventProcessor processor;
    private final AnalyticsIngestService ingestService;

    public AnalyticsEventListener(IdempotentEventProcessor processor, AnalyticsIngestService ingestService) {
        this.processor = processor;
        this.ingestService = ingestService;
    }

    @KafkaListener(topics = {
            Topics.ORDERS,
            Topics.PAYMENTS,
            Topics.INVENTORY,
            Topics.WAREHOUSES,
            Topics.SHIPMENTS,
            Topics.NOTIFICATIONS
    })
    public void onEvent(DomainEvent event) {
        if (event == null || event.eventId() == null) {
            return;
        }
        if (event.correlationId() != null) {
            MDC.put("correlationId", event.correlationId());
        }
        try {
            processor.process(event, ingestService::ingest);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
