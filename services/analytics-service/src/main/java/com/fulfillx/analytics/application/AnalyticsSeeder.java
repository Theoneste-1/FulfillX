package com.fulfillx.analytics.application;

import com.fulfillx.analytics.domain.DailyOrderMetrics;
import com.fulfillx.analytics.domain.FactOrder;
import com.fulfillx.analytics.domain.WarehouseSnapshot;
import com.fulfillx.analytics.infrastructure.persistence.DailyOrderMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.FactOrderRepository;
import com.fulfillx.analytics.infrastructure.persistence.ProductMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.WarehouseMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.WarehouseSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class AnalyticsSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsSeeder.class);

    private static final UUID WAREHOUSE_KGL = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID WAREHOUSE_NBO = UUID.fromString("11111111-1111-1111-1111-111111111112");
    private static final UUID PRODUCT_MOUSE = UUID.fromString("22222222-2222-2222-2222-222222222221");
    private static final UUID PRODUCT_SHIRT = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final FactOrderRepository facts;
    private final DailyOrderMetricsRepository dailyMetrics;
    private final WarehouseSnapshotRepository warehouses;
    private final WarehouseMetricsRepository warehouseMetrics;
    private final ProductMetricsRepository productMetrics;

    public AnalyticsSeeder(
            FactOrderRepository facts,
            DailyOrderMetricsRepository dailyMetrics,
            WarehouseSnapshotRepository warehouses,
            WarehouseMetricsRepository warehouseMetrics,
            ProductMetricsRepository productMetrics
    ) {
        this.facts = facts;
        this.dailyMetrics = dailyMetrics;
        this.warehouses = warehouses;
        this.warehouseMetrics = warehouseMetrics;
        this.productMetrics = productMetrics;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (facts.count() > 0 && dailyMetrics.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (dailyMetrics.count() == 0) {
            for (int i = 13; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                int created = 70 + (13 - i) * 4;
                int completed = Math.max(0, created - 12);
                int cancelled = 3 + (i % 3);
                int failed = 2 + (i % 2);
                BigDecimal revenue = BigDecimal.valueOf(created * 42.50);
                dailyMetrics.save(new DailyOrderMetrics(
                        date, created, completed, cancelled, failed, revenue, completed * 36_000L
                ));
                warehouseMetrics.increment(WAREHOUSE_KGL, date, completed / 2, failed, created / 2);
                warehouseMetrics.increment(WAREHOUSE_NBO, date, completed / 2, 0, created / 2);
                productMetrics.increment(PRODUCT_MOUSE, "FX-MOUSE-01", date, created, created);
                productMetrics.increment(PRODUCT_SHIRT, "FX-SHIRT-01", date, Math.max(1, created / 3), created / 3);
            }
        }
        seedFacts();
        log.info("Seeded analytics metrics and sample fact_orders");
    }

    private void seedFacts() {
        if (facts.count() > 0) {
            return;
        }
        if (warehouses.count() == 0) {
            warehouses.save(WarehouseSnapshot.upsert(WAREHOUSE_KGL, "KGL-01", "Kigali Hub", 500, 410, "OPERATIONAL"));
            warehouses.save(WarehouseSnapshot.upsert(WAREHOUSE_NBO, "NBO-01", "Nairobi Hub", 350, 120, "OPERATIONAL"));
        }
        Instant now = Instant.now();
        facts.save(delivered("SEED-DEL-1", WAREHOUSE_KGL, now.minus(2, ChronoUnit.DAYS), 89.99));
        facts.save(delivered("SEED-DEL-2", WAREHOUSE_NBO, now.minus(1, ChronoUnit.DAYS), 49.99));
        facts.save(shippedDelayed("SEED-DLY-1", WAREHOUSE_KGL, now.minus(18, ChronoUnit.HOURS), 129.50));
        facts.save(pending("SEED-PND-1", "PAYMENT_PENDING", now.minus(45, ChronoUnit.MINUTES), 24.00));
        facts.save(cancelled("SEED-CAN-1", now.minus(3, ChronoUnit.DAYS), 64.00));
        facts.save(failed("SEED-FAIL-1", now.minus(6, ChronoUnit.HOURS), 19.99));
        facts.save(pending("SEED-PRC-1", "PROCESSING", now.minus(20, ChronoUnit.MINUTES), 74.50));
        facts.save(shipped("SEED-SHP-1", WAREHOUSE_NBO, now.minus(5, ChronoUnit.HOURS), 39.00));
    }

    private FactOrder delivered(String number, UUID warehouseId, Instant createdAt, double amount) {
        FactOrder fact = base(number, warehouseId, createdAt, amount, "DELIVERED");
        fact.setPaidAt(createdAt.plus(2, ChronoUnit.MINUTES));
        fact.setReservedAt(createdAt.plus(5, ChronoUnit.MINUTES));
        fact.setShippedAt(createdAt.plus(4, ChronoUnit.HOURS));
        fact.setDeliveredAt(createdAt.plus(36, ChronoUnit.HOURS));
        return fact;
    }

    private FactOrder shippedDelayed(String number, UUID warehouseId, Instant createdAt, double amount) {
        FactOrder fact = shipped(number, warehouseId, createdAt, amount);
        fact.setDelayed(true);
        return fact;
    }

    private FactOrder shipped(String number, UUID warehouseId, Instant createdAt, double amount) {
        FactOrder fact = base(number, warehouseId, createdAt, amount, "SHIPPED");
        fact.setPaidAt(createdAt.plus(2, ChronoUnit.MINUTES));
        fact.setReservedAt(createdAt.plus(5, ChronoUnit.MINUTES));
        fact.setShippedAt(createdAt.plus(3, ChronoUnit.HOURS));
        return fact;
    }

    private FactOrder pending(String number, String status, Instant createdAt, double amount) {
        return base(number, null, createdAt, amount, status);
    }

    private FactOrder cancelled(String number, Instant createdAt, double amount) {
        FactOrder fact = base(number, WAREHOUSE_KGL, createdAt, amount, "CANCELLED");
        fact.setCancelledAt(createdAt.plus(1, ChronoUnit.HOURS));
        return fact;
    }

    private FactOrder failed(String number, Instant createdAt, double amount) {
        FactOrder fact = base(number, null, createdAt, amount, "PAYMENT_FAILED");
        fact.setFailed(true);
        return fact;
    }

    private FactOrder base(String number, UUID warehouseId, Instant createdAt, double amount, String status) {
        FactOrder fact = FactOrder.create(UUID.nameUUIDFromBytes(number.getBytes()), createdAt);
        fact.setOrderNumber(number);
        fact.setWarehouseId(warehouseId);
        fact.setTotalAmount(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP));
        fact.setCurrency("USD");
        fact.setStatus(status);
        return fact;
    }
}
