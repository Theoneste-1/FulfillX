package com.fulfillx.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_snapshot")
@IdClass(StockSnapshotId.class)
public class StockSnapshot {
    @Id
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(length = 64)
    private String sku;

    @Column(nullable = false)
    private int available;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockSnapshot() {
    }

    public static StockSnapshot of(UUID warehouseId, UUID productId, String sku, int available) {
        StockSnapshot snapshot = new StockSnapshot();
        snapshot.warehouseId = warehouseId;
        snapshot.productId = productId;
        snapshot.sku = sku;
        snapshot.available = available;
        snapshot.updatedAt = Instant.now();
        return snapshot;
    }

    public void apply(String sku, int available) {
        this.sku = sku;
        this.available = available;
        this.updatedAt = Instant.now();
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
