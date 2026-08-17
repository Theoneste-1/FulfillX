package com.fulfillx.inventory.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo catalog SKUs aligned with catalog-service seeding. Product UUIDs come from
 * Catalog (public GET) when available; otherwise a stable name-based UUID is used
 * and remapped on the first reservation that carries the real catalog productId.
 */
public final class DemoCatalog {
    public static final List<String> SKUS = List.of(
            "FX-MOUSE-01",
            "FX-KB-01",
            "FX-HUB-01",
            "FX-TEE-01",
            "FX-BIN-01",
            "FX-BOTTLE-01"
    );

    public static final Map<String, String> WAREHOUSE_CODES = Map.of(
            "KGL-01", "Kigali Hub",
            "NBO-01", "Nairobi Hub",
            "KLA-01", "Kampala Hub"
    );

    private DemoCatalog() {
    }

    public static UUID placeholderProductId(String sku) {
        return UUID.nameUUIDFromBytes(("fulfillx-sku:" + sku).getBytes());
    }

    public static int demoQuantity(String warehouseCode, String sku) {
        int hash = (warehouseCode + ":" + sku).hashCode();
        return 80 + Math.floorMod(hash, 121);
    }
}
