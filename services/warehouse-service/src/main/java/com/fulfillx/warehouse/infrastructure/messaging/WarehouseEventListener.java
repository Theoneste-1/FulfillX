package com.fulfillx.warehouse.infrastructure.messaging;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.idempotency.IdempotentEventProcessor;
import com.fulfillx.warehouse.application.WorkloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WarehouseEventListener {
    private static final Logger log = LoggerFactory.getLogger(WarehouseEventListener.class);

    private final IdempotentEventProcessor processor;
    private final WorkloadService workloadService;

    public WarehouseEventListener(IdempotentEventProcessor processor, WorkloadService workloadService) {
        this.processor = processor;
        this.workloadService = workloadService;
    }

    @KafkaListener(topics = Topics.INVENTORY, groupId = "warehouse-service")
    public void onInventoryEvent(DomainEvent event) {
        processor.process(event, this::handleInventory);
    }

    @KafkaListener(topics = Topics.SHIPMENTS, groupId = "warehouse-service")
    public void onShipmentEvent(DomainEvent event) {
        processor.process(event, this::handleShipment);
    }

    private void handleInventory(DomainEvent event) {
        switch (event.eventType()) {
            case EventTypes.INVENTORY_RESERVED -> workloadService.incrementFromReservation(event);
            case EventTypes.INVENTORY_RELEASED -> workloadService.decrementFromRelease(event);
            default -> log.debug("Ignoring inventory event {}", event.eventType());
        }
    }

    private void handleShipment(DomainEvent event) {
        if (EventTypes.SHIPMENT_DELIVERED.equals(event.eventType())) {
            workloadService.decrementFromDelivery(event);
        } else {
            log.debug("Ignoring shipment event {}", event.eventType());
        }
    }
}
