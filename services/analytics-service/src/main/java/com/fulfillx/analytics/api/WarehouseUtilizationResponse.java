package com.fulfillx.analytics.api;

import java.math.BigDecimal;
import java.util.UUID;

public record WarehouseUtilizationResponse(
        UUID warehouseId,
        String code,
        String name,
        int capacity,
        int currentWorkload,
        BigDecimal utilization,
        long ordersProcessed
) {
}
