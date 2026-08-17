package com.fulfillx.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {
    public enum Status { ACTIVE, RELEASED, CONSUMED, EXPIRED }

    @Id
    private UUID id;
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ReservationItem> items = new ArrayList<>();

    public static InventoryReservation create(UUID orderId, UUID warehouseId) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.id = UUID.randomUUID();
        reservation.orderId = orderId;
        reservation.warehouseId = warehouseId;
        reservation.status = Status.ACTIVE;
        Instant now = Instant.now();
        reservation.createdAt = now;
        reservation.expiresAt = now.plus(Duration.ofMinutes(15));
        return reservation;
    }

    public void addItem(ReservationItem item) {
        item.setReservation(this);
        items.add(item);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getWarehouseId() { return warehouseId; }
    public Status getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ReservationItem> getItems() { return items; }
}
