package com.fulfillx.order.infrastructure.messaging;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.idempotency.IdempotentEventProcessor;
import com.fulfillx.order.application.OrderEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumers {
    private final IdempotentEventProcessor idempotentEventProcessor;
    private final OrderEventService orderEventService;

    public OrderEventConsumers(
            IdempotentEventProcessor idempotentEventProcessor,
            OrderEventService orderEventService
    ) {
        this.idempotentEventProcessor = idempotentEventProcessor;
        this.orderEventService = orderEventService;
    }

    @KafkaListener(topics = Topics.PAYMENTS, groupId = "order-service")
    public void onPaymentEvent(DomainEvent event) {
        idempotentEventProcessor.process(event, this::handlePayment);
    }

    @KafkaListener(topics = Topics.INVENTORY, groupId = "order-service")
    public void onInventoryEvent(DomainEvent event) {
        idempotentEventProcessor.process(event, this::handleInventory);
    }

    @KafkaListener(topics = Topics.SHIPMENTS, groupId = "order-service")
    public void onShipmentEvent(DomainEvent event) {
        idempotentEventProcessor.process(event, this::handleShipment);
    }

    private void handlePayment(DomainEvent event) {
        switch (event.eventType()) {
            case EventTypes.PAYMENT_COMPLETED -> orderEventService.onPaymentCompleted(event);
            case EventTypes.PAYMENT_FAILED -> orderEventService.onPaymentFailed(event);
            case EventTypes.PAYMENT_REFUNDED -> orderEventService.onPaymentRefunded(event);
            default -> {
            }
        }
    }

    private void handleInventory(DomainEvent event) {
        switch (event.eventType()) {
            case EventTypes.INVENTORY_RESERVED -> orderEventService.onInventoryReserved(event);
            case EventTypes.INVENTORY_RESERVATION_FAILED -> orderEventService.onInventoryReservationFailed(event);
            case EventTypes.INVENTORY_RELEASED -> orderEventService.onInventoryReleased(event);
            default -> {
            }
        }
    }

    private void handleShipment(DomainEvent event) {
        switch (event.eventType()) {
            case EventTypes.SHIPMENT_CREATED -> orderEventService.onShipmentCreated(event);
            case EventTypes.SHIPMENT_DELIVERED -> orderEventService.onShipmentDelivered(event);
            default -> {
            }
        }
    }
}
