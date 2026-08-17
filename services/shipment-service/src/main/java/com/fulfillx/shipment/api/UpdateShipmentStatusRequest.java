package com.fulfillx.shipment.api;

import com.fulfillx.shipment.domain.Shipment;
import jakarta.validation.constraints.NotNull;

public record UpdateShipmentStatusRequest(
        @NotNull Shipment.Status status,
        String location,
        String description
) {
}
