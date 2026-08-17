package com.fulfillx.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {
    @Id
    private UUID id;
    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;
    @Column(nullable = false, length = 32)
    private String type;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "reference_id")
    private UUID referenceId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static InventoryMovement of(UUID inventoryId, String type, int quantity, UUID referenceId) {
        InventoryMovement movement = new InventoryMovement();
        movement.id = UUID.randomUUID();
        movement.inventoryId = inventoryId;
        movement.type = type;
        movement.quantity = quantity;
        movement.referenceId = referenceId;
        movement.createdAt = Instant.now();
        return movement;
    }
}
