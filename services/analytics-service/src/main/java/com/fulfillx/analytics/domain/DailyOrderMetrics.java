package com.fulfillx.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_order_metrics")
public class DailyOrderMetrics {
    @Id
    private LocalDate date;

    @Column(name = "orders_created", nullable = false)
    private int ordersCreated;

    @Column(name = "orders_completed", nullable = false)
    private int ordersCompleted;

    @Column(name = "orders_cancelled", nullable = false)
    private int ordersCancelled;

    @Column(name = "orders_failed", nullable = false)
    private int ordersFailed;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal revenue;

    @Column(name = "fulfillment_time_seconds")
    private Long fulfillmentTimeSeconds;

    protected DailyOrderMetrics() {
    }

    public DailyOrderMetrics(
            LocalDate date,
            int ordersCreated,
            int ordersCompleted,
            int ordersCancelled,
            int ordersFailed,
            BigDecimal revenue,
            Long fulfillmentTimeSeconds
    ) {
        this.date = date;
        this.ordersCreated = ordersCreated;
        this.ordersCompleted = ordersCompleted;
        this.ordersCancelled = ordersCancelled;
        this.ordersFailed = ordersFailed;
        this.revenue = revenue;
        this.fulfillmentTimeSeconds = fulfillmentTimeSeconds;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getOrdersCreated() {
        return ordersCreated;
    }

    public int getOrdersCompleted() {
        return ordersCompleted;
    }

    public int getOrdersCancelled() {
        return ordersCancelled;
    }

    public int getOrdersFailed() {
        return ordersFailed;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public Long getFulfillmentTimeSeconds() {
        return fulfillmentTimeSeconds;
    }
}
