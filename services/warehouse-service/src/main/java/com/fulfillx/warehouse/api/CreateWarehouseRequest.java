package com.fulfillx.warehouse.api;

import com.fulfillx.warehouse.domain.WarehouseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateWarehouseRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String country,
        @NotBlank String city,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull Integer capacity,
        @NotEmpty Set<String> supportedRegions,
        WarehouseStatus status
) {
}
