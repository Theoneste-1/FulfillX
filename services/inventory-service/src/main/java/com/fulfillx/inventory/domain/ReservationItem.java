package com.fulfillx.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "inventory_reservation_items")
public class ReservationItem {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private InventoryReservation reservation;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(nullable = false, length = 64)
    private String sku;
    @Column(nullable = false)
    private int quantity;

    public static ReservationItem of(UUID productId, String sku, int quantity) {
        ReservationItem item = new ReservationItem();
        item.id = UUID.randomUUID();
        item.productId = productId;
        item.sku = sku;
        item.quantity = quantity;
        return item;
    }

    public void setReservation(InventoryReservation reservation) {
        this.reservation = reservation;
    }

    public UUID getProductId() { return productId; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
