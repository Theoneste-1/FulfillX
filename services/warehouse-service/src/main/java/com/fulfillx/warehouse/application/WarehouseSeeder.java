package com.fulfillx.warehouse.application;

import com.fulfillx.warehouse.domain.Warehouse;
import com.fulfillx.warehouse.domain.WarehouseStatus;
import com.fulfillx.warehouse.infrastructure.persistence.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class WarehouseSeeder {
    private static final Logger log = LoggerFactory.getLogger(WarehouseSeeder.class);

    private final WarehouseRepository warehouses;
    private final WarehouseService warehouseService;

    public WarehouseSeeder(WarehouseRepository warehouses, WarehouseService warehouseService) {
        this.warehouses = warehouses;
        this.warehouseService = warehouseService;
    }

    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        seedOne("KGL-01", "Kigali Hub", "RW", "Kigali", -1.9441, 30.0619, 500, List.of("RW", "KE", "UG", "TZ", "US"));
        seedOne("NBO-01", "Nairobi Hub", "KE", "Nairobi", -1.2921, 36.8219, 400, List.of("KE", "UG", "TZ", "RW"));
        seedOne("KLA-01", "Kampala Hub", "UG", "Kampala", 0.3476, 32.5825, 300, List.of("UG", "KE", "RW"));
        log.info("Warehouse seed complete ({} hubs)", warehouses.count());
    }

    private void seedOne(
            String code,
            String name,
            String country,
            String city,
            double latitude,
            double longitude,
            int capacity,
            List<String> regions
    ) {
        if (warehouses.existsByCode(code)) {
            warehouses.findByCode(code).ifPresent(existing -> warehouseService.publishSnapshot(existing, null));
            return;
        }
        Set<String> supported = new LinkedHashSet<>(regions);
        Warehouse warehouse = Warehouse.create(
                code,
                name,
                country,
                city,
                latitude,
                longitude,
                capacity,
                supported,
                WarehouseStatus.OPERATIONAL
        );
        Warehouse saved = warehouses.save(warehouse);
        warehouseService.publishSnapshot(saved, null);
        log.info("Seeded warehouse {}", code);
    }
}
