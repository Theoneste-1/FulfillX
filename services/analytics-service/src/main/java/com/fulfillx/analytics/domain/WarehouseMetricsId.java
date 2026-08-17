package com.fulfillx.analytics.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class WarehouseMetricsId implements Serializable {
    private UUID warehouseId;
    private LocalDate date;

    public WarehouseMetricsId() {
    }

    public WarehouseMetricsId(UUID warehouseId, LocalDate date) {
        this.warehouseId = warehouseId;
        this.date = date;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WarehouseMetricsId that)) {
            return false;
        }
        return Objects.equals(warehouseId, that.warehouseId) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, date);
    }
}
