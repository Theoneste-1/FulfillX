package com.fulfillx.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "warehouse_cache")
public class WarehouseCache {
    @Id
    @Column(name = "warehouse_id")
    private UUID warehouseId;
    @Column(nullable = false, length = 32)
    private String code;
    @Column(nullable = false, length = 2)
    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
    @Column(nullable = false)
    private int capacity;
    @Column(name = "current_workload", nullable = false)
    private int currentWorkload;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "supported_regions", nullable = false, columnDefinition = "TEXT")
    private String supportedRegions;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static WarehouseCache fromEvent(
            UUID id, String code, String country, String city, Double lat, Double lon,
            int capacity, int workload, String status, List<String> regions
    ) {
        WarehouseCache cache = new WarehouseCache();
        cache.warehouseId = id;
        cache.code = code;
        cache.country = country;
        cache.city = city;
        cache.latitude = lat;
        cache.longitude = lon;
        cache.capacity = capacity;
        cache.currentWorkload = workload;
        cache.status = status;
        cache.supportedRegions = String.join(",", regions);
        cache.updatedAt = Instant.now();
        return cache;
    }

    public void updateFrom(
            String code, String country, String city, Double lat, Double lon,
            int capacity, int workload, String status, List<String> regions
    ) {
        this.code = code;
        this.country = country;
        this.city = city;
        this.latitude = lat;
        this.longitude = lon;
        this.capacity = capacity;
        this.currentWorkload = workload;
        this.status = status;
        this.supportedRegions = String.join(",", regions);
        this.updatedAt = Instant.now();
    }

    public List<String> regions() {
        if (supportedRegions == null || supportedRegions.isBlank()) {
            return List.of();
        }
        return Arrays.stream(supportedRegions.split(",")).map(String::trim).toList();
    }

    public boolean isOperational() {
        return "OPERATIONAL".equals(status);
    }

    public boolean supportsCountry(String destinationCountry) {
        if (destinationCountry == null) {
            return false;
        }
        return regions().stream().anyMatch(region -> region.equalsIgnoreCase(destinationCountry));
    }

    public boolean hasCapacity() {
        return currentWorkload < capacity;
    }

    public double utilization() {
        if (capacity <= 0) {
            return 1.0;
        }
        return (double) currentWorkload / (double) capacity;
    }

    public void adjustWorkload(int delta) {
        this.currentWorkload = Math.max(0, this.currentWorkload + delta);
        this.updatedAt = Instant.now();
    }

    public static WarehouseCache fromSnapshot(
            UUID id, String code, String country, String city, Double lat, Double lon,
            int capacity, int workload, String status, List<String> regions
    ) {
        return fromEvent(id, code, country, city, lat, lon, capacity, workload, status, regions);
    }

    public String getCity() { return city; }
    public UUID getWarehouseId() { return warehouseId; }
    public String getCode() { return code; }
    public String getCountry() { return country; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public int getCapacity() { return capacity; }
    public int getCurrentWorkload() { return currentWorkload; }
    public String getStatus() { return status; }
}
