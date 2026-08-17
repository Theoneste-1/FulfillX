package com.fulfillx.analytics.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyOrderMetricsResponse(
        LocalDate date,
        int ordersCreated,
        int ordersCompleted,
        int ordersCancelled,
        int ordersFailed,
        BigDecimal revenue,
        Long fulfillmentTimeSeconds
) {
}
