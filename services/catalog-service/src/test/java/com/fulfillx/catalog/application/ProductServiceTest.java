package com.fulfillx.catalog.application;

import com.fulfillx.catalog.api.CreateProductRequest;
import com.fulfillx.catalog.api.ProductStatusRequest;
import com.fulfillx.catalog.api.UpdateProductRequest;
import com.fulfillx.catalog.domain.Product;
import com.fulfillx.catalog.domain.ProductCategory;
import com.fulfillx.catalog.infrastructure.persistence.ProductRepository;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository products;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products);
    }

    @Test
    void normalizeSkuTrimsAndUppercases() {
        assertThat(ProductService.normalizeSku("  fx-mouse-01 ")).isEqualTo("FX-MOUSE-01");
        assertThat(ProductService.normalizeSku("FX-KB-01")).isEqualTo("FX-KB-01");
        assertThat(ProductService.normalizeSku("   ")).isEqualTo("");
        assertThat(ProductService.normalizeSku(null)).isNull();
    }

    @Test
    void createRejectsDuplicateSku() {
        when(products.existsBySku("FX-MOUSE-01")).thenReturn(true);

        assertThatThrownBy(() -> service.create(createRequest(" fx-mouse-01 ", "Mouse")))
                .isInstanceOf(FulfillxException.class)
                .extracting(ex -> ((FulfillxException) ex).code())
                .isEqualTo(ErrorCode.SKU_TAKEN);
    }

    @Test
    void createPersistsNormalizedSkuAndDefaultCurrency() {
        when(products.existsBySku("FX-MOUSE-01")).thenReturn(false);
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(new CreateProductRequest(
                " fx-mouse-01 ",
                "FulfillX Vertical Mouse",
                "Ergonomic mouse",
                ProductCategory.ELECTRONICS,
                new BigDecimal("49.99"),
                null,
                new BigDecimal("0.12"),
                null
        ));

        assertThat(created.sku()).isEqualTo("FX-MOUSE-01");
        assertThat(created.currency()).isEqualTo("USD");
        assertThat(created.active()).isTrue();
        assertThat(created.price()).isEqualByComparingTo("49.99");
        verify(products).save(any(Product.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(products.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(FulfillxException.class)
                .extracting(ex -> ((FulfillxException) ex).code())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getActiveByIdRejectsInactiveProduct() {
        Product product = Product.create(
                "FX-TEE-01",
                "Logistics Tee",
                null,
                ProductCategory.APPAREL,
                new BigDecimal("24.00"),
                "USD",
                null,
                false
        );
        when(products.findById(product.getId())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.getActiveById(product.getId()))
                .isInstanceOf(FulfillxException.class)
                .extracting(ex -> ((FulfillxException) ex).code())
                .isEqualTo(ErrorCode.PRODUCT_INACTIVE);
    }

    @Test
    void updateRejectsSkuTakenByAnotherProduct() {
        Product existing = Product.create(
                "FX-HUB-01",
                "USB-C Hub",
                null,
                ProductCategory.ELECTRONICS,
                new BigDecimal("39.50"),
                "USD",
                null,
                true
        );
        when(products.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(products.existsBySkuAndIdNot("FX-MOUSE-01", existing.getId())).thenReturn(true);

        UpdateProductRequest request = new UpdateProductRequest(
                "fx-mouse-01",
                "USB-C Hub",
                "Multiport",
                ProductCategory.ELECTRONICS,
                new BigDecimal("39.50"),
                "USD",
                null,
                true
        );

        assertThatThrownBy(() -> service.update(existing.getId(), request))
                .isInstanceOf(FulfillxException.class)
                .extracting(ex -> ((FulfillxException) ex).code())
                .isEqualTo(ErrorCode.SKU_TAKEN);
    }

    @Test
    void updateStatusTogglesActive() {
        Product existing = Product.create(
                "FX-BIN-01",
                "Warehouse Bin",
                null,
                ProductCategory.HOME,
                new BigDecimal("18.75"),
                "USD",
                null,
                true
        );
        when(products.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.updateStatus(existing.getId(), new ProductStatusRequest(false));

        assertThat(updated.active()).isFalse();
    }

    private static CreateProductRequest createRequest(String sku, String name) {
        return new CreateProductRequest(
                sku,
                name,
                "desc",
                ProductCategory.ELECTRONICS,
                new BigDecimal("10.00"),
                "USD",
                null,
                true
        );
    }
}
