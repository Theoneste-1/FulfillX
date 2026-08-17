package com.fulfillx.analytics.api;

import java.util.UUID;

public record TopProductResponse(UUID productId, String sku, long unitsSold) {
}
