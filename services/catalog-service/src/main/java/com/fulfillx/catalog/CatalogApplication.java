package com.fulfillx.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {
        "com.fulfillx.catalog",
        "com.fulfillx.common.idempotency"
})
@EnableJpaRepositories(basePackages = {
        "com.fulfillx.catalog",
        "com.fulfillx.common.idempotency"
})
public class CatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
