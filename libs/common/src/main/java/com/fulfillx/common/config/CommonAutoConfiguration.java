package com.fulfillx.common.config;

import com.fulfillx.common.web.CorrelationIdFilter;
import com.fulfillx.common.web.GlobalExceptionHandler;
import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Configuration
    @ConditionalOnClass(EntityManager.class)
    @EntityScan(basePackages = "com.fulfillx.common.idempotency")
    @EnableJpaRepositories(basePackages = "com.fulfillx.common.idempotency")
    @ComponentScan(basePackages = {
            "com.fulfillx.common.idempotency",
            "com.fulfillx.common.observability",
            "com.fulfillx.common.kafka"
    })
    static class JpaAndMessagingConfiguration {
    }
}
