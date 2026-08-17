package com.fulfillx.catalog.application;

import com.fulfillx.catalog.api.CreateProductRequest;
import com.fulfillx.catalog.api.ProductResponse;
import com.fulfillx.catalog.api.ProductStatusRequest;
import com.fulfillx.catalog.api.UpdateProductRequest;
import com.fulfillx.catalog.domain.Product;
import com.fulfillx.catalog.domain.ProductCategory;
import com.fulfillx.catalog.infrastructure.persistence.ProductRepository;
import com.fulfillx.common.api.PageResponse;
import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

@Service
public class ProductService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_CURRENCY = "USD";

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public static String normalizeSku(String sku) {
        if (sku == null) {
            return null;
        }
        String normalized = sku.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? normalized : normalized;
    }

    public static String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return DEFAULT_CURRENCY;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = requireSku(request.sku());
        assertSkuAvailable(sku, null);
        Product product = Product.create(
                sku,
                request.name().trim(),
                blankToNull(request.description()),
                request.category(),
                request.price(),
                normalizeCurrency(request.currency()),
                request.weightKg(),
                request.active() == null || request.active()
        );
        try {
            return ProductResponse.from(products.save(product));
        } catch (DataIntegrityViolationException ex) {
            throw skuTaken(sku, ex);
        }
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getActiveById(UUID id) {
        Product product = requireProduct(id);
        ensureActive(product);
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(
            ProductCategory category,
            Boolean active,
            String q,
            Pageable pageable
    ) {
        Pageable bounded = bound(pageable);
        String query = StringUtils.hasText(q) ? q.trim() : null;
        return PageResponse.of(
                products.search(category, active, query, bounded).map(ProductResponse::from)
        );
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = requireProduct(id);
        String sku = requireSku(request.sku());
        assertSkuAvailable(sku, id);
        product.applyUpdate(
                sku,
                request.name().trim(),
                blankToNull(request.description()),
                request.category(),
                request.price(),
                normalizeCurrency(request.currency()),
                request.weightKg(),
                request.active()
        );
        try {
            return ProductResponse.from(products.save(product));
        } catch (DataIntegrityViolationException ex) {
            throw skuTaken(sku, ex);
        }
    }

    @Transactional
    public ProductResponse updateStatus(UUID id, ProductStatusRequest request) {
        Product product = requireProduct(id);
        product.setActive(request.active());
        return ProductResponse.from(products.save(product));
    }

    public void ensureActive(Product product) {
        if (!product.isActive()) {
            throw new FulfillxException(
                    ErrorCode.PRODUCT_INACTIVE,
                    "Product " + product.getSku() + " is inactive"
            );
        }
    }

    private Product requireProduct(UUID id) {
        return products.findById(id).orElseThrow(() -> new FulfillxException(
                ErrorCode.PRODUCT_NOT_FOUND,
                "Product " + id + " was not found"
        ));
    }

    private String requireSku(String sku) {
        String normalized = normalizeSku(sku);
        if (!StringUtils.hasText(normalized)) {
            throw new FulfillxException(ErrorCode.VALIDATION_FAILED, "SKU is required");
        }
        return normalized;
    }

    private void assertSkuAvailable(String sku, UUID currentId) {
        boolean taken = currentId == null
                ? products.existsBySku(sku)
                : products.existsBySkuAndIdNot(sku, currentId);
        if (taken) {
            throw skuTaken(sku, null);
        }
    }

    private static FulfillxException skuTaken(String sku, Throwable cause) {
        return new FulfillxException(ErrorCode.SKU_TAKEN, "SKU " + sku + " is already in use", cause);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static Pageable bound(Pageable pageable) {
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), size, pageable.getSort());
    }
}
