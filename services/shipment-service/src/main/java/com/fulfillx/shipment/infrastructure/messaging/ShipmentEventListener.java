package com.fulfillx.shipment.infrastructure.messaging;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.idempotency.IdempotentEventProcessor;
import com.fulfillx.shipment.application.ShipmentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ShipmentEventListener {
    private final IdempotentEventProcessor processor;
    private final ShipmentService shipments;

    public ShipmentEventListener(IdempotentEventProcessor processor, ShipmentService shipments) {
        this.processor = processor;
        this.shipments = shipments;
    }

    @KafkaListener(topics = Topics.ORDERS, groupId = "shipment-service")
    public void onOrder(DomainEvent event) {
        processor.process(event, this::handle);
    }

    private void handle(DomainEvent event) {
        if (EventTypes.SHIPMENT_REQUESTED.equals(event.eventType())) {
            UUID orderId = UUID.fromString(event.payload().get("orderId").toString());
            Object warehouse = event.payload().get("warehouseId");
            UUID warehouseId = warehouse == null ? null : UUID.fromString(warehouse.toString());
            shipments.createFromRequest(orderId, warehouseId, event.eventId().toString());
        } else if (EventTypes.ORDER_CANCELLED.equals(event.eventType())) {
            UUID orderId = UUID.fromString(event.payload().get("orderId").toString());
            shipments.cancelIfNotShipped(orderId, event.eventId().toString());
        }
    }
}
