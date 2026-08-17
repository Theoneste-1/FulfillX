package com.fulfillx.inventory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseSelectorTest {
    @Test
    void lowerScoreWinsThenWarehouseCode() {
        WarehouseCache kgl = warehouse("KGL-01", -1.9441, 30.0619, 0, 500);
        WarehouseCache kla = warehouse("KLA-01", 0.3476, 32.5825, 0, 300);
        List<WarehouseSelector.RankedWarehouse> ranked = WarehouseSelector.rank(
                List.of(
                        new WarehouseSelector.RankedWarehouse(kla, 100, 0),
                        new WarehouseSelector.RankedWarehouse(kgl, 100, 0)
                ),
                "RW"
        );
        assertThat(ranked.get(0).warehouse().getCode()).isEqualTo("KGL-01");
    }

    @Test
    void tieBreaksOnWarehouseCode() {
        WarehouseCache a = warehouse("AAA-01", -1.94, 30.06, 0, 100);
        WarehouseCache b = warehouse("BBB-01", -1.94, 30.06, 0, 100);
        List<WarehouseSelector.RankedWarehouse> ranked = WarehouseSelector.rank(
                List.of(
                        new WarehouseSelector.RankedWarehouse(b, 50, 0),
                        new WarehouseSelector.RankedWarehouse(a, 50, 0)
                ),
                "RW"
        );
        assertThat(ranked.get(0).warehouse().getCode()).isEqualTo("AAA-01");
    }

    @Test
    void operationalRegionAndCapacityAreRequired() {
        WarehouseCache closed = WarehouseCache.fromSnapshot(
                UUID.randomUUID(), "KGL-01", "RW", "Kigali", -1.94, 30.06, 500, 10, "MAINTENANCE", List.of("RW")
        );
        WarehouseCache full = WarehouseCache.fromSnapshot(
                UUID.randomUUID(), "NBO-01", "KE", "Nairobi", -1.29, 36.82, 10, 10, "OPERATIONAL", List.of("KE")
        );
        WarehouseCache ok = WarehouseCache.fromSnapshot(
                UUID.randomUUID(), "KLA-01", "UG", "Kampala", 0.34, 32.58, 300, 1, "OPERATIONAL", List.of("UG", "RW")
        );
        assertThat(WarehouseSelector.passesHardFilters(closed, "RW")).isFalse();
        assertThat(WarehouseSelector.passesHardFilters(full, "KE")).isFalse();
        assertThat(WarehouseSelector.passesHardFilters(ok, "RW")).isTrue();
    }

    private static WarehouseCache warehouse(String code, double lat, double lon, int workload, int capacity) {
        return WarehouseCache.fromSnapshot(
                UUID.randomUUID(),
                code,
                "RW",
                "City",
                lat,
                lon,
                capacity,
                workload,
                "OPERATIONAL",
                List.of("RW", "KE", "UG")
        );
    }
}
