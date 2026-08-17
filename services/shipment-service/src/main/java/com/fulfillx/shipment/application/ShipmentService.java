package com.fulfillx.shipment.application;

import com.fulfillx.common.api.PageResponse;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.observability.FulfillxMetrics;
import com.fulfillx.outbox.OutboxWriter;
import com.fulfillx.shipment.api.ShipmentResponse;
import com.fulfillx.shipment.api.UpdateShipmentStatusRequest;
import com.fulfillx.shipment.domain.Shipment;
import com.fulfillx.shipment.domain.ShipmentTransitions;
import com.fulfillx.shipment.infrastructure.persistence.ShipmentRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShipmentService {
    public static final String METRIC_DELAYS = "fulfillx.shipments.delays";

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final ShipmentRepository shipments;
    private final OutboxWriter outbox;
    private final FulfillxMetrics metrics;

    public ShipmentService(ShipmentRepository shipments, OutboxWriter outbox, FulfillxMetrics metrics) {
        this.shipments = shipments;
        this.outbox = outbox;
        this.metrics = metrics;
    }

    @Transactional
    public void createFromRequest(UUID orderId, UUID warehouseId, String causationId) {
        if (shipments.existsByOrderId(orderId)) {
            log.info("Shipment already exists for order {}", orderId);
            return;
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("warehouseId is required to create a shipment");
        }
        Shipment shipment = Shipment.create(orderId, warehouseId);
        shipments.save(shipment);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("shipmentId", shipment.getId().toString());
        payload.put("orderId", orderId.toString());
        payload.put("warehouseId", warehouseId.toString());
        payload.put("carrier", shipment.getCarrier());
        payload.put("trackingNumber", shipment.getTrackingNumber());
        payload.put("status", shipment.getStatus().name());
        payload.put("estimatedDelivery", shipment.getEstimatedDelivery().toString());
        outbox.enqueue(EventTypes.SHIPMENT_CREATED, "Shipment", shipment.getId().toString(), payload, causationId);
    }

    @Transactional
    public ShipmentResponse changeStatus(UUID shipmentId, UpdateShipmentStatusRequest request, String causationId) {
        Shipment shipment = shipments.findById(shipmentId)
                .orElseThrow(() -> new FulfillxException(ErrorCode.SHIPMENT_NOT_FOUND, "Shipment not found"));
        applyStatus(shipment, request.status(), request.location(), request.description(), causationId);
        return ShipmentResponse.from(shipment);
    }

    @Transactional
    public void cancelIfNotShipped(UUID orderId, String causationId) {
        shipments.findByOrderId(orderId).ifPresent(shipment -> {
            if (!shipment.getStatus().isTerminal()) {
                applyStatus(shipment, Shipment.Status.FAILED, null, "Order cancelled", causationId);
            }
        });
    }

    @Transactional
    public void autoProgress() {
        List<Shipment> open = shipments.findByStatusIn(List.of(
                Shipment.Status.CREATED,
                Shipment.Status.ASSIGNED,
                Shipment.Status.PICKED_UP,
                Shipment.Status.IN_TRANSIT,
                Shipment.Status.OUT_FOR_DELIVERY
        ));
        for (Shipment shipment : open) {
            Shipment.Status next = ShipmentTransitions.nextAutoProgress(shipment.getStatus());
            if (next != null) {
                applyStatus(shipment, next, null, "Demo auto-progress", null);
            }
        }
    }

    @Transactional
    public void detectDelays() {
        publishDelays();
    }

    @Transactional
    public void publishDelays() {
        List<Shipment> delayed = shipments.findByEstimatedDeliveryBeforeAndStatusNotAndDelayEventPublishedFalse(
                Instant.now(),
                Shipment.Status.DELIVERED
        );
        for (Shipment shipment : delayed) {
            if (shipment.getStatus() == Shipment.Status.FAILED) {
                continue;
            }
            shipment.markDelayPublished();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("shipmentId", shipment.getId().toString());
            payload.put("orderId", shipment.getOrderId().toString());
            payload.put("estimatedDelivery", shipment.getEstimatedDelivery().toString());
            payload.put("status", shipment.getStatus().name());
            outbox.enqueue(EventTypes.SHIPMENT_DELAYED, "Shipment", shipment.getId().toString(), payload, null);
            metrics.increment(METRIC_DELAYS);
        }
    }

    public ShipmentResponse get(UUID id) {
        Shipment shipment = shipments.findById(id)
                .orElseThrow(() -> new FulfillxException(ErrorCode.SHIPMENT_NOT_FOUND, "Shipment not found"));
        return ShipmentResponse.from(shipment);
    }

    public PageResponse<ShipmentResponse> list(Shipment.Status status, UUID warehouseId, Boolean delayed, Pageable pageable) {
        Pageable capped = pageable.getPageSize() > 100
                ? org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort())
                : pageable;
        Specification<Shipment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (warehouseId != null) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }
            if (Boolean.TRUE.equals(delayed)) {
                predicates.add(cb.lessThan(root.get("estimatedDelivery"), Instant.now()));
                predicates.add(cb.notEqual(root.get("status"), Shipment.Status.DELIVERED));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<ShipmentResponse> page = shipments.findAll(spec, capped).map(ShipmentResponse::summary);
        return PageResponse.of(page);
    }

    private void applyStatus(
            Shipment shipment,
            Shipment.Status newStatus,
            String location,
            String description,
            String causationId
    ) {
        Shipment.Status oldStatus = shipment.getStatus();
        if (oldStatus == newStatus) {
            return;
        }
        if (!ShipmentTransitions.canTransition(oldStatus, newStatus)) {
            throw new FulfillxException(
                    ErrorCode.ILLEGAL_SHIPMENT_TRANSITION,
                    "Cannot transition shipment from " + oldStatus + " to " + newStatus
            );
        }
        shipment.advanceTo(newStatus, location, description == null ? "Status changed to " + newStatus : description);
        if (newStatus == Shipment.Status.DELIVERED) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("shipmentId", shipment.getId().toString());
            payload.put("orderId", shipment.getOrderId().toString());
            payload.put("actualDelivery", shipment.getActualDelivery().toString());
            outbox.enqueue(EventTypes.SHIPMENT_DELIVERED, "Shipment", shipment.getId().toString(), payload, causationId);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("shipmentId", shipment.getId().toString());
        payload.put("orderId", shipment.getOrderId().toString());
        payload.put("oldStatus", oldStatus.name());
        payload.put("newStatus", newStatus.name());
        payload.put("location", location);
        payload.put("description", description);
        outbox.enqueue(EventTypes.SHIPMENT_STATUS_CHANGED, "Shipment", shipment.getId().toString(), payload, causationId);
    }
}
