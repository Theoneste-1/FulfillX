package com.fulfillx.inventory.infrastructure.messaging;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.event.Topics;
import com.fulfillx.common.idempotency.IdempotentEventProcessor;
import com.fulfillx.inventory.application.InventoryService;
import com.fulfillx.inventory.domain.WarehouseCache;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class InventoryEventListener {
    private final IdempotentEventProcessor processor;
    private final InventoryService inventory;

    public InventoryEventListener(IdempotentEventProcessor processor, InventoryService inventory) {
        this.processor = processor;
        this.inventory = inventory;
    }

    @KafkaListener(topics = Topics.ORDERS, groupId = "inventory-service")
    public void onOrder(DomainEvent event) {
        processor.process(event, this::handleOrder);
    }

    @KafkaListener(topics = Topics.WAREHOUSES, groupId = "inventory-service")
    public void onWarehouse(DomainEvent event) {
        processor.process(event, this::handleWarehouse);
    }

    @KafkaListener(topics = Topics.SHIPMENTS, groupId = "inventory-service")
    public void onShipment(DomainEvent event) {
        processor.process(event, this::handleShipment);
    }

    @SuppressWarnings("unchecked")
    private void handleOrder(DomainEvent event) {
        Map<String, Object> payload = event.payload();
        UUID orderId = UUID.fromString(payload.get("orderId").toString());
        if (EventTypes.INVENTORY_RESERVATION_REQUESTED.equals(event.eventType())) {
            String country = payload.getOrDefault("destinationCountry", "US").toString();
            List<InventoryService.RequestedItem> items = new ArrayList<>();
            Object rawItems = payload.get("items");
            if (rawItems instanceof List<?> list) {
                for (Object row : list) {
                    if (row instanceof Map<?, ?> map) {
                        items.add(new InventoryService.RequestedItem(
                                UUID.fromString(map.get("productId").toString()),
                                map.get("sku").toString(),
                                ((Number) map.get("quantity")).intValue()
                        ));
                    }
                }
            }
            inventory.reserve(orderId, country, items, event.eventId().toString());
        } else if (EventTypes.ORDER_CANCELLED.equals(event.eventType())) {
            inventory.release(orderId, "CANCELLED", event.eventId().toString());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleWarehouse(DomainEvent event) {
        if (!EventTypes.WAREHOUSE_UPSERTED.equals(event.eventType())) {
            return;
        }
        Map<String, Object> payload = event.payload();
        List<String> regions = new ArrayList<>();
        Object raw = payload.get("supportedRegions");
        if (raw instanceof List<?> list) {
            list.forEach(v -> regions.add(v.toString()));
        }
        inventory.upsertWarehouse(WarehouseCache.fromEvent(
                UUID.fromString(payload.get("warehouseId").toString()),
                payload.get("code").toString(),
                payload.get("country").toString(),
                payload.getOrDefault("city", "").toString(),
                toDouble(payload.get("latitude")),
                toDouble(payload.get("longitude")),
                toInt(payload.get("capacity")),
                toInt(payload.get("currentWorkload")),
                payload.get("status").toString(),
                regions
        ));
    }

    private void handleShipment(DomainEvent event) {
        if (EventTypes.SHIPMENT_DELIVERED.equals(event.eventType())) {
            inventory.consumeOnDelivered(UUID.fromString(event.payload().get("orderId").toString()));
        }
    }

    private Double toDouble(Object value) {
        return value == null ? null : Double.valueOf(value.toString());
    }

    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }
}
