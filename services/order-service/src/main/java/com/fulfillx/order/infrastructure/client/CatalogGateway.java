package com.fulfillx.order.infrastructure.client;

import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CatalogGateway {
    private final CatalogClient catalogClient;

    public CatalogGateway(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    public CatalogProduct requireActiveProduct(UUID productId) {
        CatalogProduct product;
        try {
            product = catalogClient.getProduct(productId);
        } catch (FulfillxException ex) {
            throw ex;
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                throw new FulfillxException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + productId, ex);
            }
            throw new FulfillxException(ErrorCode.CATALOG_UNAVAILABLE, "Catalog service is unavailable", ex);
        } catch (Exception ex) {
            throw new FulfillxException(ErrorCode.CATALOG_UNAVAILABLE, "Catalog service is unavailable", ex);
        }
        if (product == null || product.id() == null) {
            throw new FulfillxException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + productId);
        }
        if (!Boolean.TRUE.equals(product.active())) {
            throw new FulfillxException(
                    ErrorCode.PRODUCT_INACTIVE,
                    "Product " + product.sku() + " is inactive"
            );
        }
        return product;
    }
}
