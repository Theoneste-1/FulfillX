package com.fulfillx.inventory.infrastructure.persistence;

import com.fulfillx.inventory.domain.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    Optional<InventoryReservation> findByOrderId(UUID orderId);

    List<InventoryReservation> findByStatusAndExpiresAtBefore(InventoryReservation.Status status, Instant cutoff);
}
