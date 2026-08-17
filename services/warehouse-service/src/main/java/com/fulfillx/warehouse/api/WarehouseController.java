package com.fulfillx.warehouse.api;

import com.fulfillx.common.security.Roles;
import com.fulfillx.warehouse.application.WarehouseService;
import com.fulfillx.warehouse.domain.WarehouseStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {
    private static final String READ_ROLES =
            "hasAnyRole('" + Roles.WAREHOUSE_OPERATOR + "','" + Roles.LOGISTICS_OPERATOR + "','"
                    + Roles.ADMIN + "','" + Roles.SUPPORT + "')";

    private final WarehouseService warehouses;

    public WarehouseController(WarehouseService warehouses) {
        this.warehouses = warehouses;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        WarehouseResponse created = warehouses.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(created);
    }

    @GetMapping
    @PreAuthorize(READ_ROLES)
    public List<WarehouseResponse> list(
            @RequestParam(required = false) WarehouseStatus status,
            @RequestParam(required = false) String country
    ) {
        return warehouses.list(status, country);
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ_ROLES)
    public WarehouseResponse get(@PathVariable UUID id) {
        return warehouses.getById(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public WarehouseResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody WarehouseStatusRequest request) {
        return warehouses.updateStatus(id, request);
    }

    @GetMapping("/{id}/workload")
    @PreAuthorize(READ_ROLES)
    public WarehouseWorkloadResponse workload(@PathVariable UUID id) {
        return warehouses.workload(id);
    }
}
