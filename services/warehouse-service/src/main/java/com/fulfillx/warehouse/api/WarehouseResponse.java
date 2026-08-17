package com.fulfillx.warehouse.api;

import com.fulfillx.warehouse.domain.Warehouse;
import com.fulfillx.warehouse.domain.WarehouseStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WarehouseResponse(
        UUID id,
        String code,
        String name,
        String country,
        String city,
        double latitude,
        double longitude,
        int capacity,
        int currentWorkload,
        WarehouseStatus status,
        List<String> supportedRegions,
        Instant createdAt,
        Instant updatedAt
) {
    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getCountry(),
                warehouse.getCity(),
                warehouse.getLatitude(),
                warehouse.getLongitude(),
                warehouse.getCapacity(),
                warehouse.getCurrentWorkload(),
                warehouse.getStatus(),
                warehouse.getSupportedRegions().stream().sorted().toList(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}
