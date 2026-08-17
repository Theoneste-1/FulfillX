package com.fulfillx.analytics.api;

import java.math.BigDecimal;
import java.time.Instant;

public record OverviewResponse(
        long ordersTotal,
        long ordersPending,
        long ordersDelayed,
        long ordersFailed,
        BigDecimal revenue,
        String currency,
        Instant generatedAt
) {
}
