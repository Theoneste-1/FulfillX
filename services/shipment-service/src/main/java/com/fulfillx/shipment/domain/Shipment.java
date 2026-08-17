package com.fulfillx.shipment.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {
    public enum Status {
        CREATED, ASSIGNED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, DELAYED, FAILED;

        public boolean isTerminal() {
            return this == DELIVERED || this == FAILED;
        }
    }

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    private UUID id;
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    @Column(nullable = false, length = 32)
    private String carrier;
    @Column(name = "tracking_number", nullable = false, unique = true, length = 64)
    private String trackingNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "estimated_delivery", nullable = false)
    private Instant estimatedDelivery;
    @Column(name = "actual_delivery")
    private Instant actualDelivery;
    @Column(name = "delay_event_published", nullable = false)
    private boolean delayEventPublished;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ShipmentEvent> events = new ArrayList<>();

    public static Shipment create(UUID orderId, UUID warehouseId) {
        Shipment shipment = new Shipment();
        shipment.id = UUID.randomUUID();
        shipment.orderId = orderId;
        shipment.warehouseId = warehouseId;
        shipment.carrier = "FXL";
        shipment.trackingNumber = tracking();
        shipment.status = Status.CREATED;
        Instant now = Instant.now();
        shipment.estimatedDelivery = now.plus(3, ChronoUnit.DAYS);
        shipment.createdAt = now;
        shipment.updatedAt = now;
        shipment.addEvent(Status.CREATED, "Origin warehouse", "Shipment created");
        return shipment;
    }

    public void advanceTo(Status next, String location, String description) {
        this.status = next;
        this.updatedAt = Instant.now();
        if (next == Status.DELIVERED) {
            this.actualDelivery = Instant.now();
        }
        addEvent(next, location, description);
    }

    public void markDelayPublished() {
        this.delayEventPublished = true;
    }

    private void addEvent(Status status, String location, String description) {
        ShipmentEvent event = ShipmentEvent.of(this, status, location, description);
        events.add(event);
    }

    private static String tracking() {
        StringBuilder sb = new StringBuilder("FX-FXL-");
        for (int i = 0; i < 12; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getWarehouseId() { return warehouseId; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public Status getStatus() { return status; }
    public Instant getEstimatedDelivery() { return estimatedDelivery; }
    public Instant getActualDelivery() { return actualDelivery; }
    public boolean isDelayEventPublished() { return delayEventPublished; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ShipmentEvent> getEvents() { return events; }
}
