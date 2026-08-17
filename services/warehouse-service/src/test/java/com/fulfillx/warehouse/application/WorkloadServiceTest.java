package com.fulfillx.warehouse.application;

import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.outbox.OutboxWriter;
import com.fulfillx.warehouse.api.CreateWarehouseRequest;
import com.fulfillx.warehouse.api.WarehouseStatusRequest;
import com.fulfillx.warehouse.domain.Warehouse;
import com.fulfillx.warehouse.domain.WarehouseOpenLoad;
import com.fulfillx.warehouse.domain.WarehouseStatus;
import com.fulfillx.warehouse.infrastructure.persistence.WarehouseOpenLoadRepository;
import com.fulfillx.warehouse.infrastructure.persistence.WarehouseRepository;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {
    @Mock
    private WarehouseRepository warehouses;
    @Mock
    private WarehouseOpenLoadRepository openLoads;
    @Mock
    private OutboxWriter outboxWriter;

    private WorkloadService workloadService;
    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseService(warehouses, outboxWriter);
        workloadService = new WorkloadService(warehouses, openLoads, warehouseService);
    }

    @Test
    void utilizationIsRoundedToTwoDecimals() {
        assertThat(WarehouseService.utilization(410, 500)).isEqualByComparingTo("0.82");
        assertThat(WarehouseService.utilization(0, 500)).isEqualByComparingTo("0.00");
        assertThat(WarehouseService.utilization(10, 0)).isEqualByComparingTo("0.00");
    }

    @Test
    void incrementRaisesWorkloadAndTracksOrder() {
        Warehouse warehouse = kigali();
        UUID orderId = UUID.randomUUID();
        when(openLoads.existsById(orderId)).thenReturn(false);
        when(warehouses.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(warehouses.save(warehouse)).thenReturn(warehouse);

        workloadService.incrementFromReservation(event(EventTypes.INVENTORY_RESERVED, Map.of(
                "orderId", orderId.toString(),
                "warehouseId", warehouse.getId().toString()
        )));

        assertThat(warehouse.getCurrentWorkload()).isEqualTo(1);
        verify(openLoads).save(any(WarehouseOpenLoad.class));
        verify(outboxWriter).enqueue(eq(EventTypes.WAREHOUSE_UPSERTED), eq("Warehouse"), eq(warehouse.getId().toString()), any(), any());
    }

    @Test
    void decrementClampsWorkloadAtZero() {
        Warehouse warehouse = kigali();
        warehouse.setCurrentWorkload(0);
        UUID orderId = UUID.randomUUID();
        when(openLoads.findById(orderId)).thenReturn(Optional.of(
                new WarehouseOpenLoad(orderId, warehouse.getId(), Instant.now())
        ));
        when(warehouses.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(warehouses.save(warehouse)).thenReturn(warehouse);

        workloadService.decrementFromRelease(event(EventTypes.INVENTORY_RELEASED, Map.of(
                "orderId", orderId.toString(),
                "warehouseId", warehouse.getId().toString()
        )));

        assertThat(warehouse.getCurrentWorkload()).isZero();
        verify(openLoads).delete(any(WarehouseOpenLoad.class));
    }

    @Test
    void decrementFromDeliveryUsesTrackedWarehouseWhenPayloadOmitsIt() {
        Warehouse warehouse = kigali();
        warehouse.setCurrentWorkload(3);
        UUID orderId = UUID.randomUUID();
        when(openLoads.findById(orderId)).thenReturn(Optional.of(
                new WarehouseOpenLoad(orderId, warehouse.getId(), Instant.now())
        ));
        when(warehouses.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(warehouses.save(warehouse)).thenReturn(warehouse);

        workloadService.decrementFromDelivery(event(EventTypes.SHIPMENT_DELIVERED, Map.of(
                "orderId", orderId.toString(),
                "shipmentId", UUID.randomUUID().toString()
        )));

        assertThat(warehouse.getCurrentWorkload()).isEqualTo(2);
    }

    @Test
    void duplicateReservationDoesNotDoubleCount() {
        UUID orderId = UUID.randomUUID();
        when(openLoads.existsById(orderId)).thenReturn(true);

        workloadService.incrementFromReservation(event(EventTypes.INVENTORY_RESERVED, Map.of(
                "orderId", orderId.toString(),
                "warehouseId", UUID.randomUUID().toString()
        )));

        verify(warehouses, never()).save(any());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(warehouses.existsByCode("KGL-01")).thenReturn(true);
        CreateWarehouseRequest request = new CreateWarehouseRequest(
                " kgl-01 ",
                "Kigali Hub",
                "RW",
                "Kigali",
                -1.9441,
                30.0619,
                500,
                java.util.Set.of("RW", "KE"),
                WarehouseStatus.OPERATIONAL
        );

        assertThatThrownBy(() -> warehouseService.create(request))
                .isInstanceOf(FulfillxException.class)
                .extracting(ex -> ((FulfillxException) ex).code())
                .isEqualTo(ErrorCode.WAREHOUSE_CODE_TAKEN);
    }

    @Test
    void missingWarehouseThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(warehouses.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getById(id))
                .isInstanceOf(FulfillxException.class)
                .extracting(ex -> ((FulfillxException) ex).code())
                .isEqualTo(ErrorCode.WAREHOUSE_NOT_FOUND);
    }

    @Test
    void updateStatusPersistsAndPublishes() {
        Warehouse warehouse = kigali();
        when(warehouses.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(warehouses.save(warehouse)).thenReturn(warehouse);

        var updated = warehouseService.updateStatus(
                warehouse.getId(),
                new WarehouseStatusRequest(WarehouseStatus.MAINTENANCE)
        );

        assertThat(updated.status()).isEqualTo(WarehouseStatus.MAINTENANCE);
        verify(outboxWriter).enqueue(eq(EventTypes.WAREHOUSE_UPSERTED), eq("Warehouse"), eq(warehouse.getId().toString()), any(), any());
    }

    @Test
    void workloadResponseIncludesUtilization() {
        Warehouse warehouse = kigali();
        warehouse.setCurrentWorkload(410);
        when(warehouses.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));

        var response = warehouseService.workload(warehouse.getId());

        assertThat(response.capacity()).isEqualTo(500);
        assertThat(response.currentWorkload()).isEqualTo(410);
        assertThat(response.utilization()).isEqualByComparingTo(new BigDecimal("0.82"));
    }

    private static Warehouse kigali() {
        return Warehouse.create(
                "KGL-01",
                "Kigali Hub",
                "RW",
                "Kigali",
                -1.9441,
                30.0619,
                500,
                java.util.Set.of("RW", "KE", "UG", "TZ", "US"),
                WarehouseStatus.OPERATIONAL
        );
    }

    private static DomainEvent event(String type, Map<String, Object> payload) {
        return DomainEvent.of(type, "Order", UUID.randomUUID().toString(), "corr", null, payload);
    }
}
