package com.fulfillx.outbox;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.json.Jsons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository repository,
            KafkaTemplate<String, DomainEvent> kafkaTemplate
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${fulfillx.outbox.poll-ms:500}")
    @Transactional
    public void publishBatch() {
        List<OutboxEvent> unpublished = repository.findUnpublished();
        for (OutboxEvent row : unpublished) {
            try {
                DomainEvent event = Jsons.read(row.getPayload(), DomainEvent.class);
                kafkaTemplate.send(topicFor(row.getEventType()), row.getAggregateId(), event).get();
                row.markPublished();
            } catch (Exception ex) {
                row.incrementAttempts();
                log.warn("Outbox publish failed for {} {}: {}", row.getEventType(), row.getId(), ex.getMessage());
            }
        }
    }

    static String topicFor(String eventType) {
        return switch (eventType) {
            case EventTypes.ORDER_CREATED, EventTypes.ORDER_STATUS_CHANGED, EventTypes.ORDER_CANCELLED,
                 EventTypes.INVENTORY_RESERVATION_REQUESTED, EventTypes.SHIPMENT_REQUESTED,
                 EventTypes.REFUND_REQUESTED -> Topics.ORDERS;
            case EventTypes.PAYMENT_COMPLETED, EventTypes.PAYMENT_FAILED, EventTypes.PAYMENT_REFUNDED -> Topics.PAYMENTS;
            case EventTypes.INVENTORY_RESERVED, EventTypes.INVENTORY_RESERVATION_FAILED,
                 EventTypes.INVENTORY_RELEASED, EventTypes.INVENTORY_STOCK_CHANGED -> Topics.INVENTORY;
            case EventTypes.WAREHOUSE_UPSERTED -> Topics.WAREHOUSES;
            case EventTypes.SHIPMENT_CREATED, EventTypes.SHIPMENT_STATUS_CHANGED,
                 EventTypes.SHIPMENT_DELAYED, EventTypes.SHIPMENT_DELIVERED -> Topics.SHIPMENTS;
            case EventTypes.NOTIFICATION_DISPATCHED -> Topics.NOTIFICATIONS;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
