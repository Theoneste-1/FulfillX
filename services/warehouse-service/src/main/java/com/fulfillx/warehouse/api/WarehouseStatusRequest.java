package com.fulfillx.warehouse.api;

import com.fulfillx.warehouse.domain.WarehouseStatus;
import jakarta.validation.constraints.NotNull;

public record WarehouseStatusRequest(@NotNull WarehouseStatus status) {
}
