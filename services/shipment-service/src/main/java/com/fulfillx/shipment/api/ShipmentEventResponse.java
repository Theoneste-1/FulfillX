package com.fulfillx.shipment.api;

import com.fulfillx.shipment.domain.Shipment;
import com.fulfillx.shipment.domain.ShipmentEvent;

import java.time.Instant;
import java.util.UUID;

public record ShipmentEventResponse(
        UUID id,
        Shipment.Status status,
        String location,
        String description,
        Instant occurredAt
) {
    public static ShipmentEventResponse from(ShipmentEvent event) {
        return new ShipmentEventResponse(
                event.getId(),
                event.getStatus(),
                event.getLocation(),
                event.getDescription(),
                event.getOccurredAt()
        );
    }
}
