package com.fulfillx.shipment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Shipment.Status status;
    private String location;
    private String description;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public static ShipmentEvent of(Shipment shipment, Shipment.Status status, String location, String description) {
        ShipmentEvent event = new ShipmentEvent();
        event.id = UUID.randomUUID();
        event.shipment = shipment;
        event.status = status;
        event.location = location;
        event.description = description;
        event.occurredAt = Instant.now();
        return event;
    }

    public UUID getId() { return id; }
    public Shipment.Status getStatus() { return status; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
    public Instant getOccurredAt() { return occurredAt; }
}
