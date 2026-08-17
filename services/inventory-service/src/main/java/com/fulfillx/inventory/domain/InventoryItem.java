package com.fulfillx.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class InventoryItem {
    @Id
    private UUID id;
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(nullable = false, length = 64)
    private String sku;
    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;
    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static InventoryItem create(UUID warehouseId, UUID productId, String sku, int onHand) {
        InventoryItem item = new InventoryItem();
        item.id = UUID.randomUUID();
        item.warehouseId = warehouseId;
        item.productId = productId;
        item.sku = sku;
        item.quantityOnHand = onHand;
        item.quantityReserved = 0;
        item.updatedAt = Instant.now();
        return item;
    }

    public int available() {
        return quantityOnHand - quantityReserved;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
        this.updatedAt = Instant.now();
    }

    public void consumeShipped(int qty) {
        this.quantityOnHand -= qty;
        this.quantityReserved -= qty;
        this.updatedAt = Instant.now();
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWarehouseId() { return warehouseId; }
    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public int getQuantityOnHand() { return quantityOnHand; }
    public int getQuantityReserved() { return quantityReserved; }
    public long getVersion() { return version; }
}
