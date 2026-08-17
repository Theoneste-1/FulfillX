package com.fulfillx.warehouse.application;

import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.json.Jsons;
import com.fulfillx.outbox.OutboxWriter;
import com.fulfillx.warehouse.api.CreateWarehouseRequest;
import com.fulfillx.warehouse.api.WarehouseResponse;
import com.fulfillx.warehouse.api.WarehouseStatusRequest;
import com.fulfillx.warehouse.api.WarehouseWorkloadResponse;
import com.fulfillx.warehouse.domain.Warehouse;
import com.fulfillx.warehouse.domain.WarehouseStatus;
import com.fulfillx.warehouse.event.WarehouseUpsertedPayload;
import com.fulfillx.warehouse.infrastructure.persistence.WarehouseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WarehouseService {
    private static final String AGGREGATE_TYPE = "Warehouse";

    private final WarehouseRepository warehouses;
    private final OutboxWriter outboxWriter;

    public WarehouseService(WarehouseRepository warehouses, OutboxWriter outboxWriter) {
        this.warehouses = warehouses;
        this.outboxWriter = outboxWriter;
    }

    public static String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizeCountry(String country) {
        if (country == null) {
            return null;
        }
        return country.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        String code = requireCode(request.code());
        if (warehouses.existsByCode(code)) {
            throw codeTaken(code, null);
        }
        Warehouse warehouse = Warehouse.create(
                code,
                request.name().trim(),
                normalizeCountry(request.country()),
                request.city().trim(),
                request.latitude(),
                request.longitude(),
                request.capacity(),
                regions(request.supportedRegions()),
                request.status() == null ? WarehouseStatus.OPERATIONAL : request.status()
        );
        try {
            Warehouse saved = warehouses.save(warehouse);
            enqueueUpsert(saved, null);
            return WarehouseResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw codeTaken(code, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> list(WarehouseStatus status, String country) {
        String normalizedCountry = StringUtils.hasText(country) ? normalizeCountry(country) : null;
        return warehouses.findAll().stream()
                .filter(warehouse -> status == null || warehouse.getStatus() == status)
                .filter(warehouse -> matchesCountry(warehouse, normalizedCountry))
                .sorted(Comparator.comparing(Warehouse::getCode))
                .map(WarehouseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getById(UUID id) {
        return WarehouseResponse.from(requireWarehouse(id));
    }

    @Transactional
    public WarehouseResponse updateStatus(UUID id, WarehouseStatusRequest request) {
        Warehouse warehouse = requireWarehouse(id);
        warehouse.setStatus(request.status());
        Warehouse saved = warehouses.save(warehouse);
        enqueueUpsert(saved, null);
        return WarehouseResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public WarehouseWorkloadResponse workload(UUID id) {
        Warehouse warehouse = requireWarehouse(id);
        return new WarehouseWorkloadResponse(
                warehouse.getId(),
                warehouse.getCapacity(),
                warehouse.getCurrentWorkload(),
                utilization(warehouse.getCurrentWorkload(), warehouse.getCapacity())
        );
    }

    public Warehouse requireWarehouse(UUID id) {
        return warehouses.findById(id).orElseThrow(() -> new FulfillxException(
                ErrorCode.WAREHOUSE_NOT_FOUND,
                "Warehouse " + id + " was not found"
        ));
    }

    public void enqueueUpsert(Warehouse warehouse, String causationId) {
        outboxWriter.enqueue(
                EventTypes.WAREHOUSE_UPSERTED,
                AGGREGATE_TYPE,
                warehouse.getId().toString(),
                Jsons.toMap(WarehouseUpsertedPayload.from(warehouse)),
                causationId
        );
    }

    public void publishSnapshot(Warehouse warehouse, String causationId) {
        enqueueUpsert(warehouse, causationId);
    }

    private String requireCode(String code) {
        String normalized = normalizeCode(code);
        if (!StringUtils.hasText(normalized)) {
            throw new FulfillxException(ErrorCode.VALIDATION_FAILED, "Warehouse code is required");
        }
        return normalized;
    }

    private static boolean matchesCountry(Warehouse warehouse, String country) {
        if (country == null) {
            return true;
        }
        return country.equals(warehouse.getCountry()) || warehouse.getSupportedRegions().contains(country);
    }

    private static Set<String> regions(Collection<String> supportedRegions) {
        return supportedRegions.stream()
                .map(WarehouseService::normalizeCountry)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static FulfillxException codeTaken(String code, Throwable cause) {
        return new FulfillxException(ErrorCode.WAREHOUSE_CODE_TAKEN, "Warehouse code " + code + " is already in use", cause);
    }

    static BigDecimal utilization(int currentWorkload, int capacity) {
        if (capacity <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(currentWorkload)
                .divide(BigDecimal.valueOf(capacity), 2, RoundingMode.HALF_UP);
    }
}
