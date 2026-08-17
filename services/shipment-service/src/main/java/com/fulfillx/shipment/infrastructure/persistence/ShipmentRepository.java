package com.fulfillx.shipment.infrastructure.persistence;

import com.fulfillx.shipment.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID>, JpaSpecificationExecutor<Shipment> {
    Optional<Shipment> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    List<Shipment> findByStatus(Shipment.Status status);

    List<Shipment> findByStatusIn(List<Shipment.Status> statuses);

    List<Shipment> findByEstimatedDeliveryBeforeAndStatusNotAndDelayEventPublishedFalse(
            Instant cutoff,
            Shipment.Status delivered
    );
}
