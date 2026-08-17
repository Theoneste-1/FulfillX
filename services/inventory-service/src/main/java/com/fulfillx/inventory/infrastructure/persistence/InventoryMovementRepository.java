package com.fulfillx.inventory.infrastructure.persistence;

import com.fulfillx.inventory.domain.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
}
