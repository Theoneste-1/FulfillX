package com.fulfillx.warehouse.event;

import com.fulfillx.warehouse.domain.Warehouse;

import java.util.List;
import java.util.UUID;

public record WarehouseUpsertedPayload(
        UUID warehouseId,
        String code,
        String name,
        String country,
        String city,
        double latitude,
        double longitude,
        int capacity,
        int currentWorkload,
        String status,
        List<String> supportedRegions
) {
    public static WarehouseUpsertedPayload from(Warehouse warehouse) {
        return new WarehouseUpsertedPayload(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getCountry(),
                warehouse.getCity(),
                warehouse.getLatitude(),
                warehouse.getLongitude(),
                warehouse.getCapacity(),
                warehouse.getCurrentWorkload(),
                warehouse.getStatus().name(),
                warehouse.getSupportedRegions().stream().sorted().toList()
        );
    }
}
