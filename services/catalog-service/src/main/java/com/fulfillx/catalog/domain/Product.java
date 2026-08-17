package com.fulfillx.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ProductCategory category;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "weight_kg", precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product() {
    }

    public static Product create(
            String sku,
            String name,
            String description,
            ProductCategory category,
            BigDecimal price,
            String currency,
            BigDecimal weightKg,
            boolean active
    ) {
        Product product = new Product();
        product.id = UUID.randomUUID();
        product.sku = sku;
        product.name = name;
        product.description = description;
        product.category = category;
        product.price = scalePrice(price);
        product.currency = currency;
        product.weightKg = scaleWeight(weightKg);
        product.active = active;
        return product;
    }

    public void applyUpdate(
            String sku,
            String name,
            String description,
            ProductCategory category,
            BigDecimal price,
            String currency,
            BigDecimal weightKg,
            boolean active
    ) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = scalePrice(price);
        this.currency = currency;
        this.weightKg = scaleWeight(weightKg);
        this.active = active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static BigDecimal scalePrice(BigDecimal price) {
        return price == null ? null : price.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal scaleWeight(BigDecimal weightKg) {
        return weightKg == null ? null : weightKg.setScale(3, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
