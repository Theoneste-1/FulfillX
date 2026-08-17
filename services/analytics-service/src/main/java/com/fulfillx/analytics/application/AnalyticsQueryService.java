package com.fulfillx.analytics.application;

import com.fulfillx.analytics.api.AlertItemResponse;
import com.fulfillx.analytics.api.AlertsResponse;
import com.fulfillx.analytics.api.DailyOrderMetricsResponse;
import com.fulfillx.analytics.api.OrdersByStatusResponse;
import com.fulfillx.analytics.api.OverviewResponse;
import com.fulfillx.analytics.api.StatusCountResponse;
import com.fulfillx.analytics.api.TopProductResponse;
import com.fulfillx.analytics.api.WarehouseUtilizationResponse;
import com.fulfillx.analytics.domain.DailyOrderMetrics;
import com.fulfillx.analytics.domain.OpsAlert;
import com.fulfillx.analytics.domain.WarehouseSnapshot;
import com.fulfillx.analytics.infrastructure.persistence.DailyOrderMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.FactOrderRepository;
import com.fulfillx.analytics.infrastructure.persistence.OpsAlertRepository;
import com.fulfillx.analytics.infrastructure.persistence.ProductMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.WarehouseMetricsRepository;
import com.fulfillx.analytics.infrastructure.persistence.WarehouseSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsQueryService {
    private final FactOrderRepository facts;
    private final DailyOrderMetricsRepository dailyMetrics;
    private final WarehouseSnapshotRepository warehouses;
    private final WarehouseMetricsRepository warehouseMetrics;
    private final ProductMetricsRepository productMetrics;
    private final OpsAlertRepository alerts;

    public AnalyticsQueryService(
            FactOrderRepository facts,
            DailyOrderMetricsRepository dailyMetrics,
            WarehouseSnapshotRepository warehouses,
            WarehouseMetricsRepository warehouseMetrics,
            ProductMetricsRepository productMetrics,
            OpsAlertRepository alerts
    ) {
        this.facts = facts;
        this.dailyMetrics = dailyMetrics;
        this.warehouses = warehouses;
        this.warehouseMetrics = warehouseMetrics;
        this.productMetrics = productMetrics;
        this.alerts = alerts;
    }

    @Transactional(readOnly = true)
    public OverviewResponse overview() {
        return new OverviewResponse(
                facts.count(),
                facts.countPending(),
                facts.countByDelayedTrue(),
                facts.countByFailedTrue(),
                facts.sumPaidRevenue() == null ? BigDecimal.ZERO : facts.sumPaidRevenue(),
                "USD",
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<DailyOrderMetricsResponse> daily(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(ZoneOffset.UTC) : to;
        LocalDate start = from == null ? end.minusDays(13) : from;
        return dailyMetrics.findByDateBetweenOrderByDateAsc(start, end).stream()
                .map(this::toDaily)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrdersByStatusResponse byStatus() {
        List<StatusCountResponse> rows = facts.countByStatus().stream()
                .map(row -> new StatusCountResponse(String.valueOf(row[0]), (Long) row[1]))
                .toList();
        return new OrdersByStatusResponse(rows);
    }

    @Transactional(readOnly = true)
    public List<WarehouseUtilizationResponse> warehouseUtilization() {
        Map<UUID, Long> factCounts = new HashMap<>();
        for (Object[] row : facts.countByWarehouse()) {
            factCounts.put((UUID) row[0], (Long) row[1]);
        }
        List<WarehouseSnapshot> snapshots = warehouses.findAll();
        if (!snapshots.isEmpty()) {
            List<WarehouseUtilizationResponse> result = new ArrayList<>();
            for (WarehouseSnapshot snapshot : snapshots) {
                int capacity = snapshot.getCapacity() == null ? 0 : snapshot.getCapacity();
                int workload = snapshot.getCurrentWorkload() == null ? 0 : snapshot.getCurrentWorkload();
                BigDecimal utilization = capacity <= 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(workload).divide(BigDecimal.valueOf(capacity), 4, RoundingMode.HALF_UP);
                result.add(new WarehouseUtilizationResponse(
                        snapshot.getWarehouseId(),
                        snapshot.getCode(),
                        snapshot.getName(),
                        capacity,
                        workload,
                        utilization,
                        factCounts.getOrDefault(snapshot.getWarehouseId(), 0L)
                ));
            }
            return result;
        }
        List<WarehouseUtilizationResponse> fallback = new ArrayList<>();
        for (Object[] row : warehouseMetrics.totalsByWarehouse()) {
            UUID warehouseId = (UUID) row[0];
            long processed = row[1] == null ? 0L : ((Number) row[1]).longValue();
            fallback.add(new WarehouseUtilizationResponse(
                    warehouseId,
                    null,
                    null,
                    0,
                    0,
                    BigDecimal.ZERO,
                    Math.max(processed, factCounts.getOrDefault(warehouseId, 0L))
            ));
        }
        for (Map.Entry<UUID, Long> entry : factCounts.entrySet()) {
            boolean present = fallback.stream().anyMatch(item -> item.warehouseId().equals(entry.getKey()));
            if (!present) {
                fallback.add(new WarehouseUtilizationResponse(
                        entry.getKey(), null, null, 0, 0, BigDecimal.ZERO, entry.getValue()
                ));
            }
        }
        return fallback;
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(int limit) {
        int size = limit <= 0 ? 10 : Math.min(limit, 100);
        return productMetrics.topProducts(size).stream()
                .map(row -> new TopProductResponse(
                        uuid(row[0]),
                        row[1] == null ? null : String.valueOf(row[1]),
                        row[2] == null ? 0L : ((Number) row[2]).longValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AlertsResponse alerts() {
        List<AlertItemResponse> items = alerts.findByOpenTrueOrderByCreatedAtDesc().stream()
                .map(this::toAlert)
                .toList();
        return new AlertsResponse(items);
    }

    private DailyOrderMetricsResponse toDaily(DailyOrderMetrics metrics) {
        return new DailyOrderMetricsResponse(
                metrics.getDate(),
                metrics.getOrdersCreated(),
                metrics.getOrdersCompleted(),
                metrics.getOrdersCancelled(),
                metrics.getOrdersFailed(),
                metrics.getRevenue(),
                metrics.getFulfillmentTimeSeconds()
        );
    }

    private AlertItemResponse toAlert(OpsAlert alert) {
        return new AlertItemResponse(alert.getSeverity(), alert.getCode(), alert.getMessage(), alert.getCreatedAt());
    }

    private static UUID uuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return value == null ? null : UUID.fromString(value.toString());
    }
}
