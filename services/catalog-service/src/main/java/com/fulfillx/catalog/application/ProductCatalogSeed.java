package com.fulfillx.catalog.application;

import com.fulfillx.catalog.domain.Product;
import com.fulfillx.catalog.domain.ProductCategory;
import com.fulfillx.catalog.infrastructure.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class ProductCatalogSeed {
    private static final Logger log = LoggerFactory.getLogger(ProductCatalogSeed.class);

    private final ProductRepository products;

    public ProductCatalogSeed(ProductRepository products) {
        this.products = products;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (products.count() > 0) {
            return;
        }
        products.save(product("FX-MOUSE-01", "FulfillX Vertical Mouse", "Ergonomic vertical mouse", ProductCategory.ELECTRONICS, "49.99", "0.120"));
        products.save(product("FX-KB-01", "Mechanical Keyboard", "Tactile mechanical keyboard", ProductCategory.ELECTRONICS, "129.00", "0.850"));
        products.save(product("FX-HUB-01", "USB-C Hub", "Multiport USB-C hub", ProductCategory.ELECTRONICS, "39.50", "0.090"));
        products.save(product("FX-TEE-01", "Logistics Tee", "Cotton logistics crew tee", ProductCategory.APPAREL, "24.00", "0.180"));
        products.save(product("FX-BIN-01", "Warehouse Bin", "Stackable warehouse storage bin", ProductCategory.HOME, "18.75", "0.450"));
        products.save(product("FX-BOTTLE-01", "Steel Bottle", "Insulated stainless steel bottle", ProductCategory.SPORTS, "22.00", "0.320"));
        log.info("Seeded {} catalog products", products.count());
    }

    private static Product product(
            String sku,
            String name,
            String description,
            ProductCategory category,
            String price,
            String weightKg
    ) {
        return Product.create(
                sku,
                name,
                description,
                category,
                new BigDecimal(price),
                "USD",
                new BigDecimal(weightKg),
                true
        );
    }
}
