package com.fulfillx.analytics.application;

import com.fulfillx.analytics.domain.FactOrder;
import com.fulfillx.analytics.domain.StockSnapshot;
import com.fulfillx.analytics.domain.StockSnapshotId;
import com.fulfillx.analytics.domain.WarehouseSnapshot;
import com.fulfillx.analytics.infrastructure.persistence.DailyOrderMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.FactOrderRepository;
import com.fulfillx.analytics.infrastructure.persistence.ProductMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.StockSnapshotRepository;
import com.fulfillx.analytics.infrastructure.persistence.WarehouseMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.WarehouseSnapshotRepository;
import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsIngestService {
    private final FactOrderRepository facts;
    private final DailyOrderMetricsRepository dailyMetrics;
    private final WarehouseMetricsRepository warehouseMetrics;
    private final ProductMetricsRepository productMetrics;
    private final WarehouseSnapshotRepository warehouses;
    private final StockSnapshotRepository stock;

    public AnalyticsIngestService(
            FactOrderRepository facts,
            DailyOrderMetricsRepository dailyMetrics,
            WarehouseMetricsRepository warehouseMetrics,
            ProductMetricsRepository productMetrics,
            WarehouseSnapshotRepository warehouses,
            StockSnapshotRepository stock
    ) {
        this.facts = facts;
        this.dailyMetrics = dailyMetrics;
        this.warehouseMetrics = warehouseMetrics;
        this.productMetrics = productMetrics;
        this.warehouses = warehouses;
        this.stock = stock;
    }

    public void ingest(DomainEvent event) {
        Map<String, Object> payload = PayloadMaps.orEmpty(event.payload());
        Instant occurredAt = event.occurredAt() == null ? Instant.now() : event.occurredAt();
        LocalDate date = occurredAt.atZone(ZoneOffset.UTC).toLocalDate();
        switch (event.eventType()) {
            case EventTypes.ORDER_CREATED -> onOrderCreated(payload, occurredAt, date);
            case EventTypes.ORDER_STATUS_CHANGED -> onStatusChanged(payload);
            case EventTypes.ORDER_CANCELLED -> onCancelled(payload, occurredAt, date);
            case EventTypes.PAYMENT_COMPLETED -> onPaymentCompleted(payload, occurredAt, date);
            case EventTypes.PAYMENT_FAILED -> onPaymentFailed(payload, occurredAt, date);
            case EventTypes.PAYMENT_REFUNDED -> onPaymentRefunded(payload, occurredAt);
            case EventTypes.INVENTORY_RESERVED -> onInventoryReserved(payload, occurredAt, date);
            case EventTypes.INVENTORY_RESERVATION_FAILED -> onInventoryFailed(payload, occurredAt, date);
            case EventTypes.INVENTORY_STOCK_CHANGED -> onStockChanged(payload);
            case EventTypes.WAREHOUSE_UPSERTED -> onWarehouseUpserted(payload);
            case EventTypes.SHIPMENT_CREATED -> onShipmentCreated(payload, occurredAt);
            case EventTypes.SHIPMENT_DELAYED -> onShipmentDelayed(payload);
            case EventTypes.SHIPMENT_DELIVERED -> onShipmentDelivered(payload, occurredAt, date);
            default -> {
            }
        }
    }

    private void onOrderCreated(Map<String, Object> payload, Instant occurredAt, LocalDate date) {
        UUID orderId = PayloadMaps.uuid(payload, "orderId");
        if (orderId == null) {
            return;
        }
        FactOrder fact = facts.findById(orderId).orElseGet(() -> FactOrder.create(orderId, occurredAt));
        fact.setOrderNumber(PayloadMaps.text(payload, "orderNumber"));
        fact.setCustomerId(PayloadMaps.uuid(payload, "customerId"));
        fact.setStatus(PayloadMaps.text(payload, "status") == null ? "PAYMENT_PENDING" : PayloadMaps.text(payload, "status"));
        fact.setTotalAmount(PayloadMaps.decimal(payload, "totalAmount"));
        String currency = PayloadMaps.text(payload, "currency");
        fact.setCurrency(currency == null ? "USD" : currency);
        facts.save(fact);
        dailyMetrics.increment(date, 1, 0, 0, 0, BigDecimal.ZERO, 0);
        for (Map<String, Object> item : PayloadMaps.listOfMaps(payload, "items")) {
            UUID productId = PayloadMaps.uuid(item, "productId");
            if (productId == null) {
                continue;
            }
            Integer quantity = PayloadMaps.integer(item, "quantity");
            productMetrics.increment(productId, PayloadMaps.text(item, "sku"), date, quantity == null ? 0 : quantity, 0);
        }
    }

    private void onStatusChanged(Map<String, Object> payload) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        String status = PayloadMaps.text(payload, "newStatus");
        if (status != null) {
            fact.setStatus(status);
        }
        facts.save(fact);
    }

    private void onCancelled(Map<String, Object> payload, Instant occurredAt, LocalDate date) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        fact.setStatus("CANCELLED");
        fact.setCancelledAt(occurredAt);
        facts.save(fact);
        dailyMetrics.increment(date, 0, 0, 1, 0, BigDecimal.ZERO, 0);
    }

    private void onPaymentCompleted(Map<String, Object> payload, Instant occurredAt, LocalDate date) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        fact.setStatus("PAID");
        fact.setPaidAt(occurredAt);
        BigDecimal amount = PayloadMaps.decimal(payload, "amount");
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            fact.setTotalAmount(amount);
        }
        String currency = PayloadMaps.text(payload, "currency");
        if (currency != null) {
            fact.setCurrency(currency);
        }
        facts.save(fact);
        dailyMetrics.increment(date, 0, 0, 0, 0, fact.getTotalAmount(), 0);
    }

    private void onPaymentFailed(Map<String, Object> payload, Instant occurredAt, LocalDate date) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        fact.setStatus("PAYMENT_FAILED");
        fact.setFailed(true);
        facts.save(fact);
        dailyMetrics.increment(date, 0, 0, 0, 1, BigDecimal.ZERO, 0);
    }

    private void onPaymentRefunded(Map<String, Object> payload, Instant occurredAt) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        fact.setStatus("CANCELLED");
        fact.setCancelledAt(occurredAt);
        facts.save(fact);
    }

    private void onInventoryReserved(Map<String, Object> payload, Instant occurredAt, LocalDate date) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        UUID warehouseId = PayloadMaps.uuid(payload, "warehouseId");
        fact.setStatus("RESERVED");
        fact.setReservedAt(occurredAt);
        fact.setWarehouseId(warehouseId);
        facts.save(fact);
        if (warehouseId != null) {
            warehouseMetrics.increment(warehouseId, date, 1, 0, 1);
        }
        for (Map<String, Object> item : PayloadMaps.listOfMaps(payload, "items")) {
            UUID productId = PayloadMaps.uuid(item, "productId");
            if (productId == null) {
                continue;
            }
            Integer quantity = PayloadMaps.integer(item, "quantity");
            productMetrics.increment(productId, PayloadMaps.text(item, "sku"), date, 0, quantity == null ? 0 : quantity);
        }
    }

    private void onInventoryFailed(Map<String, Object> payload, Instant occurredAt, LocalDate date) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        fact.setStatus("INVENTORY_FAILED");
        fact.setFailed(true);
        facts.save(fact);
        dailyMetrics.increment(date, 0, 0, 0, 1, BigDecimal.ZERO, 0);
        UUID warehouseId = fact.getWarehouseId();
        if (warehouseId != null) {
            warehouseMetrics.increment(warehouseId, date, 0, 1, 1);
        }
    }

    private void onStockChanged(Map<String, Object> payload) {
        UUID warehouseId = PayloadMaps.uuid(payload, "warehouseId");
        UUID productId = PayloadMaps.uuid(payload, "productId");
        Integer available = PayloadMaps.integer(payload, "available");
        if (warehouseId == null || productId == null || available == null) {
            return;
        }
        StockSnapshot snapshot = stock.findById(new StockSnapshotId(warehouseId, productId))
                .orElseGet(() -> StockSnapshot.of(warehouseId, productId, PayloadMaps.text(payload, "sku"), available));
        snapshot.apply(PayloadMaps.text(payload, "sku"), available);
        stock.save(snapshot);
    }

    private void onWarehouseUpserted(Map<String, Object> payload) {
        UUID warehouseId = PayloadMaps.uuid(payload, "warehouseId");
        if (warehouseId == null) {
            return;
        }
        WarehouseSnapshot snapshot = warehouses.findById(warehouseId)
                .orElseGet(() -> WarehouseSnapshot.upsert(
                        warehouseId,
                        PayloadMaps.text(payload, "code"),
                        PayloadMaps.text(payload, "name"),
                        PayloadMaps.integer(payload, "capacity"),
                        PayloadMaps.integer(payload, "currentWorkload"),
                        PayloadMaps.text(payload, "status")
                ));
        snapshot.apply(
                PayloadMaps.text(payload, "code"),
                PayloadMaps.text(payload, "name"),
                PayloadMaps.integer(payload, "capacity"),
                PayloadMaps.integer(payload, "currentWorkload"),
                PayloadMaps.text(payload, "status")
        );
        warehouses.save(snapshot);
    }

    private void onShipmentCreated(Map<String, Object> payload, Instant occurredAt) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        fact.setStatus("SHIPPED");
        fact.setShippedAt(occurredAt);
        UUID warehouseId = PayloadMaps.uuid(payload, "warehouseId");
        if (warehouseId != null) {
            fact.setWarehouseId(warehouseId);
        }
        facts.save(fact);
    }

    private void onShipmentDelayed(Map<String, Object> payload) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        fact.setDelayed(true);
        facts.save(fact);
    }

    private void onShipmentDelivered(Map<String, Object> payload, Instant occurredAt, LocalDate date) {
        FactOrder fact = requireFact(payload);
        if (fact == null) {
            return;
        }
        Instant deliveredAt = PayloadMaps.instant(payload, "actualDelivery", occurredAt);
        fact.setStatus("DELIVERED");
        fact.setDeliveredAt(deliveredAt);
        fact.setDelayed(false);
        facts.save(fact);
        long fulfillment = 0;
        if (fact.getCreatedAt() != null && deliveredAt != null) {
            fulfillment = Math.max(0, Duration.between(fact.getCreatedAt(), deliveredAt).getSeconds());
        }
        dailyMetrics.increment(date, 0, 1, 0, 0, BigDecimal.ZERO, fulfillment);
    }

    private FactOrder requireFact(Map<String, Object> payload) {
        UUID orderId = PayloadMaps.uuid(payload, "orderId");
        if (orderId == null) {
            return null;
        }
        Instant now = Instant.now();
        return facts.findById(orderId).orElseGet(() -> facts.save(FactOrder.create(orderId, now)));
    }
}
