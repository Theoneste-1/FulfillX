package com.fulfillx.shipment.api;

import com.fulfillx.common.api.PageResponse;
import com.fulfillx.shipment.application.ShipmentService;
import com.fulfillx.shipment.domain.Shipment;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {
    private final ShipmentService shipments;

    public ShipmentController(ShipmentService shipments) {
        this.shipments = shipments;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR','ADMIN','SUPPORT')")
    public PageResponse<ShipmentResponse> list(
            @RequestParam(required = false) Shipment.Status status,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) Boolean delayed,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return shipments.list(status, warehouseId, delayed, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ShipmentResponse get(@PathVariable UUID id) {
        return shipments.get(id);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LOGISTICS_OPERATOR','ADMIN')")
    public ShipmentResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateShipmentStatusRequest request
    ) {
        return shipments.changeStatus(id, request, null);
    }
}
