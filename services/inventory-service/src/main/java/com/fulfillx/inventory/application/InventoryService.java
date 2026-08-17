package com.fulfillx.inventory.application;

import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.observability.FulfillxMetrics;
import com.fulfillx.inventory.domain.DemoCatalog;
import com.fulfillx.inventory.domain.InventoryItem;
import com.fulfillx.inventory.domain.InventoryMovement;
import com.fulfillx.inventory.domain.InventoryReservation;
import com.fulfillx.inventory.domain.ReservationItem;
import com.fulfillx.inventory.domain.WarehouseCache;
import com.fulfillx.inventory.domain.WarehouseSelector;
import com.fulfillx.inventory.infrastructure.persistence.InventoryItemRepository;
import com.fulfillx.inventory.infrastructure.persistence.InventoryMovementRepository;
import com.fulfillx.inventory.infrastructure.persistence.InventoryReservationRepository;
import com.fulfillx.inventory.infrastructure.persistence.WarehouseCacheRepository;
import com.fulfillx.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {
    public record RequestedItem(UUID productId, String sku, int quantity) {
    }

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository inventory;
    private final InventoryReservationRepository reservations;
    private final InventoryMovementRepository movements;
    private final WarehouseCacheRepository warehouses;
    private final OutboxWriter outbox;
    private final FulfillxMetrics metrics;

    public InventoryService(
            InventoryItemRepository inventory,
            InventoryReservationRepository reservations,
            InventoryMovementRepository movements,
            WarehouseCacheRepository warehouses,
            OutboxWriter outbox,
            FulfillxMetrics metrics
    ) {
        this.inventory = inventory;
        this.reservations = reservations;
        this.movements = movements;
        this.warehouses = warehouses;
        this.outbox = outbox;
        this.metrics = metrics;
    }

    @Transactional
    public void reserve(UUID orderId, String destinationCountry, List<RequestedItem> items, String causationId) {
        if (reservations.findByOrderId(orderId).isPresent()) {
            return;
        }
        List<WarehouseCache> candidates = selectWarehouses(destinationCountry, items);
        for (WarehouseCache warehouse : candidates) {
            if (tryReserveInWarehouse(orderId, warehouse, items, causationId)) {
                warehouse.adjustWorkload(1);
                metrics.increment("fulfillx.inventory.reservations");
                return;
            }
        }
        metrics.increment("fulfillx.inventory.reservation.failures");
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("reason", "INSUFFICIENT_INVENTORY");
        payload.put("message", "No warehouse could fulfill the basket");
        outbox.enqueue(EventTypes.INVENTORY_RESERVATION_FAILED, "Inventory", orderId.toString(), payload, causationId);
    }

    private List<WarehouseCache> selectWarehouses(String destinationCountry, List<RequestedItem> items) {
        List<WarehouseCache> cached = warehouses.findAll();
        if (cached.isEmpty()) {
            log.warn("WAREHOUSE_SELECTION_DEGRADED warehouse_cache is empty; using stock-only selection until WarehouseUpserted events arrive");
            return inventory.findAll().stream()
                    .map(InventoryItem::getWarehouseId)
                    .distinct()
                    .filter(id -> canFulfillAll(id, items))
                    .map(id -> warehouses.findById(id).orElse(stub(id)))
                    .toList();
        }
        String country = WarehouseSelector.normalizeCountry(destinationCountry);
        List<WarehouseSelector.RankedWarehouse> eligible = new ArrayList<>();
        for (WarehouseCache warehouse : cached) {
            if (!WarehouseSelector.passesHardFilters(warehouse, country)) {
                continue;
            }
            if (!canFulfillAll(warehouse.getWarehouseId(), items)) {
                continue;
            }
            int availableSum = items.stream()
                    .mapToInt(item -> locate(warehouse.getWarehouseId(), item.productId(), item.sku()).map(InventoryItem::available).orElse(0))
                    .sum();
            eligible.add(new WarehouseSelector.RankedWarehouse(warehouse, availableSum, 0));
        }
        return WarehouseSelector.rank(eligible, country).stream().map(WarehouseSelector.RankedWarehouse::warehouse).toList();
    }

    private boolean canFulfillAll(UUID warehouseId, List<RequestedItem> items) {
        for (RequestedItem item : items) {
            Optional<InventoryItem> row = locate(warehouseId, item.productId(), item.sku());
            if (row.isEmpty() || row.get().available() < item.quantity()) {
                return false;
            }
        }
        return true;
    }

    private boolean tryReserveInWarehouse(
            UUID orderId,
            WarehouseCache warehouse,
            List<RequestedItem> items,
            String causationId
    ) {
        List<RequestedItem> reserved = new ArrayList<>();
        for (RequestedItem item : items) {
            InventoryItem row = locate(warehouse.getWarehouseId(), item.productId(), item.sku()).orElse(null);
            if (row == null) {
                reserved.forEach(done -> inventory.release(warehouse.getWarehouseId(), done.productId(), done.quantity()));
                return false;
            }
            if (!row.getProductId().equals(item.productId())) {
                row.setProductId(item.productId());
                inventory.saveAndFlush(row);
            }
            int updated = inventory.tryReserve(warehouse.getWarehouseId(), row.getProductId(), item.quantity());
            if (updated == 0) {
                reserved.forEach(done -> inventory.release(warehouse.getWarehouseId(), done.productId(), done.quantity()));
                return false;
            }
            reserved.add(new RequestedItem(row.getProductId(), item.sku(), item.quantity()));
        }
        InventoryReservation reservation = InventoryReservation.create(orderId, warehouse.getWarehouseId());
        for (RequestedItem item : reserved) {
            reservation.addItem(ReservationItem.of(item.productId(), item.sku(), item.quantity()));
            inventory.findByWarehouseIdAndProductId(warehouse.getWarehouseId(), item.productId()).ifPresent(row ->
                    movements.save(InventoryMovement.of(row.getId(), "RESERVED", item.quantity(), orderId)));
        }
        reservations.save(reservation);
        Map<String, Object> payload = new HashMap<>();
        payload.put("reservationId", reservation.getId().toString());
        payload.put("orderId", orderId.toString());
        payload.put("warehouseId", warehouse.getWarehouseId().toString());
        payload.put("warehouseCode", warehouse.getCode());
        payload.put("items", items.stream().map(i -> Map.of(
                "productId", i.productId().toString(),
                "sku", i.sku(),
                "quantity", i.quantity()
        )).toList());
        outbox.enqueue(EventTypes.INVENTORY_RESERVED, "Inventory", reservation.getId().toString(), payload, causationId);
        return true;
    }

    @Transactional
    public void release(UUID orderId, String reason, String causationId) {
        reservations.findByOrderId(orderId).ifPresent(reservation -> {
            if (reservation.getStatus() != InventoryReservation.Status.ACTIVE) {
                return;
            }
            reservation.getItems().forEach(item -> {
                inventory.release(reservation.getWarehouseId(), item.getProductId(), item.getQuantity());
                inventory.findByWarehouseIdAndProductId(reservation.getWarehouseId(), item.getProductId())
                        .ifPresent(row -> movements.save(InventoryMovement.of(row.getId(), "RELEASED", item.getQuantity(), orderId)));
            });
            reservation.setStatus("EXPIRED".equals(reason) ? InventoryReservation.Status.EXPIRED : InventoryReservation.Status.RELEASED);
            warehouses.findById(reservation.getWarehouseId()).ifPresent(cache -> cache.adjustWorkload(-1));
            Map<String, Object> payload = new HashMap<>();
            payload.put("reservationId", reservation.getId().toString());
            payload.put("orderId", orderId.toString());
            payload.put("warehouseId", reservation.getWarehouseId().toString());
            payload.put("reason", reason);
            outbox.enqueue(EventTypes.INVENTORY_RELEASED, "Inventory", reservation.getId().toString(), payload, causationId);
        });
    }

    @Transactional
    public void expireDue() {
        reservations.findByStatusAndExpiresAtBefore(InventoryReservation.Status.ACTIVE, Instant.now())
                .forEach(reservation -> release(reservation.getOrderId(), "EXPIRED", null));
    }

    @Transactional
    public void consumeOnDelivered(UUID orderId) {
        reservations.findByOrderId(orderId).ifPresent(reservation -> {
            if (reservation.getStatus() != InventoryReservation.Status.ACTIVE) {
                log.warn("Reservation {} for order {} is {}, skipping consume", reservation.getId(), orderId, reservation.getStatus());
                return;
            }
            reservation.getItems().forEach(item -> {
                inventory.consumeShipped(reservation.getWarehouseId(), item.getProductId(), item.getQuantity());
                inventory.findByWarehouseIdAndProductId(reservation.getWarehouseId(), item.getProductId())
                        .ifPresent(row -> movements.save(InventoryMovement.of(row.getId(), "SHIPPED", item.getQuantity(), orderId)));
            });
            reservation.setStatus(InventoryReservation.Status.CONSUMED);
            warehouses.findById(reservation.getWarehouseId()).ifPresent(cache -> cache.adjustWorkload(-1));
        });
    }

    @Transactional
    public InventoryItem setLevel(UUID warehouseId, UUID productId, String sku, int onHand) {
        InventoryItem item = inventory.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> InventoryItem.create(warehouseId, productId, sku, 0));
        if (onHand < item.getQuantityReserved()) {
            throw new FulfillxException(ErrorCode.CONFLICT, "Cannot set on-hand below reserved quantity");
        }
        int delta = onHand - item.getQuantityOnHand();
        item.setQuantityOnHand(onHand);
        inventory.save(item);
        if (delta != 0) {
            movements.save(InventoryMovement.of(item.getId(), delta > 0 ? "RECEIVED" : "ADJUSTED", Math.abs(delta), null));
        }
        return item;
    }

    @Transactional
    public void upsertWarehouse(WarehouseCache incoming) {
        warehouses.findById(incoming.getWarehouseId()).ifPresentOrElse(
                existing -> existing.updateFrom(
                        incoming.getCode(),
                        incoming.getCountry(),
                        incoming.getCity(),
                        incoming.getLatitude(),
                        incoming.getLongitude(),
                        incoming.getCapacity(),
                        incoming.getCurrentWorkload(),
                        incoming.getStatus(),
                        incoming.regions()
                ),
                () -> warehouses.save(incoming)
        );
        seedDemoStock(warehouses.findById(incoming.getWarehouseId()).orElse(incoming));
    }

    private void seedDemoStock(WarehouseCache warehouse) {
        for (String sku : DemoCatalog.SKUS) {
            if (inventory.existsByWarehouseIdAndSkuIgnoreCase(warehouse.getWarehouseId(), sku)) {
                continue;
            }
            int quantity = DemoCatalog.demoQuantity(warehouse.getCode(), sku);
            InventoryItem row = InventoryItem.create(
                    warehouse.getWarehouseId(),
                    DemoCatalog.placeholderProductId(sku),
                    sku,
                    quantity
            );
            inventory.save(row);
            movements.save(InventoryMovement.of(row.getId(), "RECEIVED", quantity, warehouse.getWarehouseId()));
            log.info("Seeded {} units of {} at warehouse {}", quantity, sku, warehouse.getCode());
        }
    }

    public List<InventoryItem> list(UUID warehouseId, UUID productId, boolean lowStock) {
        List<InventoryItem> items;
        if (warehouseId != null) {
            items = inventory.findByWarehouseId(warehouseId);
        } else if (productId != null) {
            items = inventory.findByProductId(productId);
        } else {
            items = inventory.findAll();
        }
        if (lowStock) {
            return items.stream().filter(i -> i.available() <= 10).toList();
        }
        return items;
    }

    public InventoryReservation reservationByOrder(UUID orderId) {
        return reservations.findByOrderId(orderId)
                .orElseThrow(() -> new FulfillxException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"));
    }

    @Transactional
    public void releaseByReservationId(UUID reservationId) {
        InventoryReservation reservation = reservations.findById(reservationId)
                .orElseThrow(() -> new FulfillxException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found"));
        release(reservation.getOrderId(), "MANUAL", null);
    }

    private Optional<InventoryItem> locate(UUID warehouseId, UUID productId, String sku) {
        Optional<InventoryItem> byProduct = inventory.findByWarehouseIdAndProductId(warehouseId, productId);
        if (byProduct.isPresent()) {
            return byProduct;
        }
        return inventory.findByWarehouseIdAndSkuIgnoreCase(warehouseId, sku);
    }

    private static WarehouseCache stub(UUID warehouseId) {
        return WarehouseCache.fromEvent(
                warehouseId,
                warehouseId.toString(),
                "ZZ",
                null,
                null,
                null,
                Integer.MAX_VALUE,
                0,
                "OPERATIONAL",
                List.of()
        );
    }
}
