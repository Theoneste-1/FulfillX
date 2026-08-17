package com.fulfillx.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EntityScan(basePackages = {
        "com.fulfillx.warehouse",
        "com.fulfillx.common.idempotency",
        "com.fulfillx.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.fulfillx.warehouse",
        "com.fulfillx.common.idempotency",
        "com.fulfillx.outbox"
})
public class WarehouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(WarehouseApplication.class, args);
    }
}
