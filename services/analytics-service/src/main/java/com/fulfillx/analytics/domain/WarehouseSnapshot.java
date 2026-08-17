package com.fulfillx.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "warehouse_snapshot")
public class WarehouseSnapshot {
    @Id
    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(length = 32)
    private String code;

    @Column(length = 200)
    private String name;

    private Integer capacity;

    @Column(name = "current_workload")
    private Integer currentWorkload;

    @Column(length = 32)
    private String status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WarehouseSnapshot() {
    }

    public static WarehouseSnapshot upsert(
            UUID warehouseId,
            String code,
            String name,
            Integer capacity,
            Integer currentWorkload,
            String status
    ) {
        WarehouseSnapshot snapshot = new WarehouseSnapshot();
        snapshot.warehouseId = warehouseId;
        snapshot.code = code;
        snapshot.name = name;
        snapshot.capacity = capacity;
        snapshot.currentWorkload = currentWorkload;
        snapshot.status = status;
        snapshot.updatedAt = Instant.now();
        return snapshot;
    }

    public void apply(String code, String name, Integer capacity, Integer currentWorkload, String status) {
        this.code = code;
        this.name = name;
        this.capacity = capacity;
        this.currentWorkload = currentWorkload;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getCurrentWorkload() {
        return currentWorkload;
    }

    public String getStatus() {
        return status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
