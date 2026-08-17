package com.fulfillx.inventory.api;

import com.fulfillx.inventory.application.InventoryService;
import com.fulfillx.inventory.domain.InventoryItem;
import com.fulfillx.inventory.domain.InventoryReservation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {
    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @PutMapping("/levels")
    @PreAuthorize("hasAnyRole('WAREHOUSE_OPERATOR','ADMIN')")
    public InventoryView setLevel(@RequestBody LevelRequest request) {
        return InventoryView.from(inventory.setLevel(request.warehouseId(), request.productId(), request.sku(), request.quantityOnHand()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAREHOUSE_OPERATOR','ADMIN','SUPPORT')")
    public List<InventoryView> list(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "false") boolean lowStock
    ) {
        return inventory.list(warehouseId, productId, lowStock).stream().map(InventoryView::from).toList();
    }

    @GetMapping("/reservations/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT','LOGISTICS_OPERATOR')")
    public ReservationView reservation(@PathVariable UUID orderId) {
        InventoryReservation reservation = inventory.reservationByOrder(orderId);
        return new ReservationView(reservation.getId(), reservation.getOrderId(), reservation.getWarehouseId(), reservation.getStatus().name());
    }

    @PostMapping("/reservations/{id}/release")
    @PreAuthorize("hasRole('ADMIN')")
    public void release(@PathVariable UUID id) {
        inventory.releaseByReservationId(id);
    }

    public record LevelRequest(@NotNull UUID warehouseId, @NotNull UUID productId, @NotBlank String sku, @Min(0) int quantityOnHand) {
    }

    public record InventoryView(UUID id, UUID warehouseId, UUID productId, String sku, int quantityOnHand, int quantityReserved, int available, long version) {
        static InventoryView from(InventoryItem item) {
            return new InventoryView(item.getId(), item.getWarehouseId(), item.getProductId(), item.getSku(),
                    item.getQuantityOnHand(), item.getQuantityReserved(), item.available(), item.getVersion());
        }
    }

    public record ReservationView(UUID id, UUID orderId, UUID warehouseId, String status) {
    }
}
