package com.fulfillx.shipment.api;

import com.fulfillx.shipment.domain.Shipment;
import com.fulfillx.shipment.domain.ShipmentEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        UUID orderId,
        UUID warehouseId,
        String carrier,
        String trackingNumber,
        Shipment.Status status,
        Instant estimatedDelivery,
        Instant actualDelivery,
        boolean delayEventPublished,
        Instant createdAt,
        Instant updatedAt,
        List<ShipmentEventResponse> events
) {
    public static ShipmentResponse from(Shipment shipment, List<ShipmentEvent> events) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getWarehouseId(),
                shipment.getCarrier(),
                shipment.getTrackingNumber(),
                shipment.getStatus(),
                shipment.getEstimatedDelivery(),
                shipment.getActualDelivery(),
                shipment.isDelayEventPublished(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt(),
                events.stream().map(ShipmentEventResponse::from).toList()
        );
    }

    public static ShipmentResponse from(Shipment shipment) {
        return from(shipment, shipment.getEvents() == null ? List.of() : shipment.getEvents());
    }

    public static ShipmentResponse summary(Shipment shipment) {
        return from(shipment, List.of());
    }
}
