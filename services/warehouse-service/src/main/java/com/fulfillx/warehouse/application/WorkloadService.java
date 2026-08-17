package com.fulfillx.warehouse.application;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.warehouse.domain.Warehouse;
import com.fulfillx.warehouse.domain.WarehouseOpenLoad;
import com.fulfillx.warehouse.infrastructure.persistence.WarehouseOpenLoadRepository;
import com.fulfillx.warehouse.infrastructure.persistence.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WorkloadService {
    private static final Logger log = LoggerFactory.getLogger(WorkloadService.class);

    private final WarehouseRepository warehouses;
    private final WarehouseOpenLoadRepository openLoads;
    private final WarehouseService warehouseService;

    public WorkloadService(
            WarehouseRepository warehouses,
            WarehouseOpenLoadRepository openLoads,
            WarehouseService warehouseService
    ) {
        this.warehouses = warehouses;
        this.openLoads = openLoads;
        this.warehouseService = warehouseService;
    }

    @Transactional
    public void incrementFromReservation(DomainEvent event) {
        UUID orderId = uuid(event.payload(), "orderId");
        UUID warehouseId = uuid(event.payload(), "warehouseId");
        if (orderId == null || warehouseId == null) {
            log.warn("Skipping InventoryReserved {} — missing orderId or warehouseId", event.eventId());
            return;
        }
        if (openLoads.existsById(orderId)) {
            log.info("Workload already tracked for order {}", orderId);
            return;
        }
        Warehouse warehouse = warehouses.findById(warehouseId).orElseThrow(() -> new FulfillxException(
                ErrorCode.WAREHOUSE_NOT_FOUND,
                "Warehouse " + warehouseId + " was not found for reservation"
        ));
        warehouse.incrementWorkload();
        openLoads.save(new WarehouseOpenLoad(orderId, warehouseId));
        warehouses.save(warehouse);
        warehouseService.enqueueUpsert(warehouse, event.eventId().toString());
    }

    @Transactional
    public void decrementFromRelease(DomainEvent event) {
        UUID orderId = uuid(event.payload(), "orderId");
        UUID warehouseId = uuid(event.payload(), "warehouseId");
        decrement(orderId, warehouseId, event);
    }

    @Transactional
    public void decrementFromDelivery(DomainEvent event) {
        UUID orderId = uuid(event.payload(), "orderId");
        UUID warehouseId = uuid(event.payload(), "warehouseId");
        decrement(orderId, warehouseId, event);
    }

    private void decrement(UUID orderId, UUID payloadWarehouseId, DomainEvent event) {
        Optional<WarehouseOpenLoad> tracked = orderId == null ? Optional.empty() : openLoads.findById(orderId);
        UUID warehouseId = tracked.map(WarehouseOpenLoad::getWarehouseId).orElse(payloadWarehouseId);
        if (warehouseId == null) {
            log.warn("Skipping workload decrement for event {} — warehouse could not be resolved", event.eventId());
            return;
        }
        if (tracked.isEmpty() && orderId != null) {
            log.info("No open load for order {} — workload already released", orderId);
            return;
        }
        Warehouse warehouse = warehouses.findById(warehouseId).orElse(null);
        if (warehouse == null) {
            log.warn("Warehouse {} missing while decrementing workload", warehouseId);
            tracked.ifPresent(openLoads::delete);
            return;
        }
        warehouse.decrementWorkload();
        warehouses.save(warehouse);
        tracked.ifPresent(openLoads::delete);
        warehouseService.enqueueUpsert(warehouse, event.eventId().toString());
    }

    static UUID uuid(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        return UUID.fromString(value.toString());
    }
}
