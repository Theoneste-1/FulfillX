package com.fulfillx.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "catalog-service", url = "${CATALOG_SERVICE_URL:http://localhost:8082}")
public interface CatalogClient {
    @GetMapping("/api/v1/products/{id}")
    CatalogProduct getProduct(@PathVariable("id") UUID id);
}
