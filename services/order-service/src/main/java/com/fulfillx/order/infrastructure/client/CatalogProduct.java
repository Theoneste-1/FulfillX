package com.fulfillx.order.infrastructure.client;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogProduct(
        UUID id,
        String sku,
        String name,
        BigDecimal price,
        String currency,
        Boolean active
) {
}
