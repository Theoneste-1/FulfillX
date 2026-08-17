package com.fulfillx.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "product_metrics")
@IdClass(ProductMetricsId.class)
public class ProductMetrics {
    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(length = 64)
    private String sku;

    @Id
    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "units_sold", nullable = false)
    private int unitsSold;

    @Column(name = "units_reserved", nullable = false)
    private int unitsReserved;

    protected ProductMetrics() {
    }

    public UUID getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getUnitsSold() {
        return unitsSold;
    }

    public int getUnitsReserved() {
        return unitsReserved;
    }
}
