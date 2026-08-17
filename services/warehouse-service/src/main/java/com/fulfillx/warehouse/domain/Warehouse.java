package com.fulfillx.warehouse.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "current_workload", nullable = false)
    private int currentWorkload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WarehouseStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "warehouse_regions", joinColumns = @JoinColumn(name = "warehouse_id"))
    @Column(name = "country", nullable = false, length = 2)
    private Set<String> supportedRegions = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Warehouse() {
    }

    public static Warehouse create(
            String code,
            String name,
            String country,
            String city,
            double latitude,
            double longitude,
            int capacity,
            Set<String> regions,
            WarehouseStatus status
    ) {
        Warehouse warehouse = new Warehouse();
        warehouse.id = UUID.randomUUID();
        warehouse.code = code;
        warehouse.name = name;
        warehouse.country = country;
        warehouse.city = city;
        warehouse.latitude = latitude;
        warehouse.longitude = longitude;
        warehouse.capacity = capacity;
        warehouse.currentWorkload = 0;
        warehouse.status = status;
        warehouse.replaceRegions(regions);
        return warehouse;
    }

    public void setStatus(WarehouseStatus status) {
        this.status = status;
    }

    public void setCurrentWorkload(int currentWorkload) {
        this.currentWorkload = Math.max(0, currentWorkload);
    }

    public void incrementWorkload() {
        adjustWorkload(1);
    }

    public void decrementWorkload() {
        adjustWorkload(-1);
    }

    public void adjustWorkload(int delta) {
        this.currentWorkload = Math.max(0, this.currentWorkload + delta);
    }

    public void replaceRegions(Set<String> regions) {
        this.supportedRegions.clear();
        if (regions != null) {
            this.supportedRegions.addAll(regions);
        }
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

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentWorkload() {
        return currentWorkload;
    }

    public WarehouseStatus getStatus() {
        return status;
    }

    public Set<String> getSupportedRegions() {
        return supportedRegions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
