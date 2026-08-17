package com.fulfillx.analytics.api;

import com.fulfillx.analytics.application.AnalyticsQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('WAREHOUSE_OPERATOR','LOGISTICS_OPERATOR','SUPPORT','ADMIN')")
public class AnalyticsController {
    private final AnalyticsQueryService queryService;

    public AnalyticsController(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/overview")
    public OverviewResponse overview() {
        return queryService.overview();
    }

    @GetMapping("/orders/daily")
    public List<DailyOrderMetricsResponse> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return queryService.daily(from, to);
    }

    @GetMapping("/orders/by-status")
    public OrdersByStatusResponse byStatus() {
        return queryService.byStatus();
    }

    @GetMapping("/warehouses/utilization")
    public List<WarehouseUtilizationResponse> warehouseUtilization() {
        return queryService.warehouseUtilization();
    }

    @GetMapping("/products/top")
    public List<TopProductResponse> topProducts(@RequestParam(defaultValue = "10") int limit) {
        return queryService.topProducts(limit);
    }

    @GetMapping("/alerts")
    public AlertsResponse alerts() {
        return queryService.alerts();
    }
}
