package com.fulfillx.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "warehouse_open_loads")
public class WarehouseOpenLoad {
    @Id
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WarehouseOpenLoad() {
    }

    public WarehouseOpenLoad(UUID orderId, UUID warehouseId) {
        this.orderId = orderId;
        this.warehouseId = warehouseId;
        this.createdAt = Instant.now();
    }

    public WarehouseOpenLoad(UUID orderId, UUID warehouseId, Instant createdAt) {
        this.orderId = orderId;
        this.warehouseId = warehouseId;
        this.createdAt = createdAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
