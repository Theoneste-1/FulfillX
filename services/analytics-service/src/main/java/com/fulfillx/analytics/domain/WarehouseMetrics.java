package com.fulfillx.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "warehouse_metrics")
@IdClass(WarehouseMetricsId.class)
public class WarehouseMetrics {
    @Id
    @Column(name = "warehouse_id", nullable = false)
    private UUID warehouseId;

    @Id
    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "orders_processed", nullable = false)
    private int ordersProcessed;

    @Column(name = "failure_rate_numerator", nullable = false)
    private int failureRateNumerator;

    @Column(name = "failure_rate_denominator", nullable = false)
    private int failureRateDenominator;

    protected WarehouseMetrics() {
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getOrdersProcessed() {
        return ordersProcessed;
    }

    public int getFailureRateNumerator() {
        return failureRateNumerator;
    }

    public int getFailureRateDenominator() {
        return failureRateDenominator;
    }
}
