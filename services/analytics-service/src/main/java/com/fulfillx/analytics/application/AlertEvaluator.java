package com.fulfillx.analytics.application;

import com.fulfillx.analytics.domain.OpsAlert;
import com.fulfillx.analytics.domain.StockSnapshot;
import com.fulfillx.analytics.domain.WarehouseSnapshot;
import com.fulfillx.analytics.infrastructure.persistence.AnalyticsProcessedEventRepository;
import com.fulfillx.analytics.infrastructure.persistence.FactOrderRepository;
import com.fulfillx.analytics.infrastructure.persistence.OpsAlertRepository;
import com.fulfillx.analytics.infrastructure.persistence.StockSnapshotRepository;
import com.fulfillx.analytics.infrastructure.persistence.WarehouseSnapshotRepository;
import com.fulfillx.common.event.EventTypes;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AlertEvaluator {
    private final FactOrderRepository facts;
    private final WarehouseSnapshotRepository warehouses;
    private final StockSnapshotRepository stock;
    private final OpsAlertRepository alerts;
    private final AnalyticsProcessedEventRepository processedEvents;

    public AlertEvaluator(
            FactOrderRepository facts,
            WarehouseSnapshotRepository warehouses,
            StockSnapshotRepository stock,
            OpsAlertRepository alerts,
            AnalyticsProcessedEventRepository processedEvents
    ) {
        this.facts = facts;
        this.warehouses = warehouses;
        this.stock = stock;
        this.alerts = alerts;
        this.processedEvents = processedEvents;
    }

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    public void evaluate() {
        long delayed = facts.countByDelayedTrue();
        upsert(
                delayed >= 10,
                delayed >= 25 ? "CRITICAL" : "WARNING",
                "DELAYED_SHIPMENTS",
                delayed + " delayed shipments"
        );

        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        long failed = processedEvents.countByTypeSince(EventTypes.PAYMENT_FAILED, hourAgo);
        long completed = processedEvents.countByTypeSince(EventTypes.PAYMENT_COMPLETED, hourAgo);
        long attempts = failed + completed;
        double rate = attempts == 0 ? 0.0 : (failed * 100.0) / attempts;
        upsert(
                attempts > 0 && rate >= 15.0,
                rate >= 30.0 ? "CRITICAL" : "WARNING",
                "PAYMENT_FAILURE_RATE",
                String.format("Payment failure rate last hour %.1f%% (%d/%d)", rate, failed, attempts)
        );

        long stuck = facts.countStuck(Instant.now().minus(30, ChronoUnit.MINUTES));
        upsert(
                stuck > 0,
                stuck >= 10 ? "CRITICAL" : "WARNING",
                "STUCK_ORDERS",
                stuck + " orders stuck in a non-terminal status for over 30 minutes"
        );

        List<WarehouseSnapshot> overloaded = warehouses.findAll().stream()
                .filter(snapshot -> snapshot.getCapacity() != null
                        && snapshot.getCapacity() > 0
                        && snapshot.getCurrentWorkload() != null
                        && snapshot.getCurrentWorkload() * 100.0 / snapshot.getCapacity() >= 80.0)
                .toList();
        String warehouseMessage = overloaded.stream()
                .map(snapshot -> {
                    double util = snapshot.getCurrentWorkload() * 100.0 / snapshot.getCapacity();
                    return (snapshot.getCode() == null ? snapshot.getWarehouseId().toString() : snapshot.getCode())
                            + String.format(" utilization %.0f%%", util);
                })
                .collect(Collectors.joining("; "));
        upsert(
                !overloaded.isEmpty(),
                overloaded.stream().anyMatch(s -> s.getCurrentWorkload() * 100.0 / s.getCapacity() >= 95.0)
                        ? "CRITICAL" : "WARNING",
                "WAREHOUSE_CAPACITY",
                overloaded.isEmpty() ? "Warehouses within capacity" : warehouseMessage
        );

        List<StockSnapshot> low = stock.findByAvailableLessThanEqual(10);
        String stockMessage = low.stream()
                .map(item -> (item.getSku() == null ? item.getProductId().toString() : item.getSku())
                        + " available=" + item.getAvailable())
                .collect(Collectors.joining("; "));
        upsert(
                !low.isEmpty(),
                low.stream().anyMatch(item -> item.getAvailable() <= 0) ? "CRITICAL" : "WARNING",
                "LOW_STOCK",
                low.isEmpty() ? "No low-stock SKUs" : stockMessage
        );
    }

    private void upsert(boolean triggered, String severity, String code, String message) {
        var existing = alerts.findFirstByCodeAndOpenTrue(code);
        if (triggered) {
            if (existing.isPresent()) {
                existing.get().refresh(severity, message);
            } else {
                alerts.save(OpsAlert.open(severity, code, message));
            }
        } else {
            existing.ifPresent(OpsAlert::resolve);
        }
    }
}
