package com.fulfillx.warehouse.api;

import java.math.BigDecimal;
import java.util.UUID;

public record WarehouseWorkloadResponse(
        UUID warehouseId,
        int capacity,
        int currentWorkload,
        BigDecimal utilization
) {
}
