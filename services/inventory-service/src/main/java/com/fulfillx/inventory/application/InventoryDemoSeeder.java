package com.fulfillx.inventory.application;

import com.fulfillx.inventory.domain.InventoryItem;
import com.fulfillx.inventory.domain.WarehouseCache;
import com.fulfillx.inventory.infrastructure.persistence.InventoryItemRepository;
import com.fulfillx.inventory.infrastructure.persistence.WarehouseCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class InventoryDemoSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InventoryDemoSeeder.class);
    private final WarehouseCacheRepository warehouses;
    private final InventoryItemRepository inventory;
    private final RestClient restClient;

    public InventoryDemoSeeder(
            WarehouseCacheRepository warehouses,
            InventoryItemRepository inventory,
            @Value("${CATALOG_SERVICE_URL:http://localhost:8082}") String catalogUrl
    ) {
        this.warehouses = warehouses;
        this.inventory = inventory;
        this.restClient = RestClient.create(catalogUrl);
    }

    @Scheduled(fixedDelay = 20_000)
    public void retrySeed() {
        try {
            seedIfPossible();
        } catch (Exception ex) {
            log.debug("Inventory demo seed retry: {}", ex.getMessage());
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Inventory waits for warehouses.events WarehouseUpserted to fill warehouse_cache, then seeds demo SKUs (80-200 units). Warehouse REST is not called (list endpoint is authenticated; Kafka consumers have no user JWT).");
        retrySeed();
    }

    private void seedIfPossible() {
        List<WarehouseCache> hubs = warehouses.findAll();
        if (hubs.isEmpty() || inventory.count() > 0) {
            return;
        }
        Map<String, Object> page = restClient.get()
                .uri("/api/v1/products?size=50")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (page == null) {
            return;
        }
        Object content = page.get("content");
        if (!(content instanceof List<?> products)) {
            return;
        }
        for (WarehouseCache warehouse : hubs) {
            for (Object row : products) {
                if (row instanceof Map<?, ?> product) {
                    UUID productId = UUID.fromString(product.get("id").toString());
                    String sku = product.get("sku").toString();
                    inventory.save(InventoryItem.create(warehouse.getWarehouseId(), productId, sku, 120));
                }
            }
        }
        log.info("Seeded demo inventory for {} warehouses", hubs.size());
    }
}
