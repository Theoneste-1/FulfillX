package com.fulfillx.catalog;

import com.fulfillx.catalog.api.CreateProductRequest;
import com.fulfillx.catalog.application.ProductService;
import com.fulfillx.catalog.domain.ProductCategory;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class CatalogApplicationIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fulfillx_catalog")
            .withUsername("fulfillx")
            .withPassword("fulfillx");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("management.tracing.enabled", () -> "false");
    }

    @Autowired
    private ProductService products;

    @Test
    void seedsCatalogAndEnforcesUniqueSku() {
        var page = products.list(null, true, "FX-MOUSE", org.springframework.data.domain.PageRequest.of(0, 20));
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(page.content())
                .extracting(item -> item.sku())
                .contains("FX-MOUSE-01");

        CreateProductRequest duplicate = new CreateProductRequest(
                " fx-mouse-01 ",
                "Clone Mouse",
                "Should fail",
                ProductCategory.ELECTRONICS,
                new BigDecimal("1.00"),
                "USD",
                null,
                true
        );
        assertThatThrownBy(() -> products.create(duplicate))
                .isInstanceOf(FulfillxException.class)
                .extracting(ex -> ((FulfillxException) ex).code())
                .isEqualTo(ErrorCode.SKU_TAKEN);

        var created = products.create(new CreateProductRequest(
                "fx-cable-01",
                "USB-C Cable",
                "1m cable",
                ProductCategory.ELECTRONICS,
                new BigDecimal("9.99"),
                "usd",
                new BigDecimal("0.04"),
                true
        ));
        assertThat(created.sku()).isEqualTo("FX-CABLE-01");
        assertThat(created.currency()).isEqualTo("USD");
        assertThat(products.getById(created.id()).name()).isEqualTo("USB-C Cable");
    }
}
