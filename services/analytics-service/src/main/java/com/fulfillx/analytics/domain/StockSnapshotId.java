package com.fulfillx.analytics.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class StockSnapshotId implements Serializable {
    private UUID warehouseId;
    private UUID productId;

    public StockSnapshotId() {
    }

    public StockSnapshotId(UUID warehouseId, UUID productId) {
        this.warehouseId = warehouseId;
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockSnapshotId that)) {
            return false;
        }
        return Objects.equals(warehouseId, that.warehouseId) && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, productId);
    }
}
