package com.fulfillx.gateway.config;

import com.fulfillx.gateway.jwt.JwtProperties;
import com.fulfillx.gateway.jwt.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class GatewayConfig {

    @Bean
    public KeyResolver rateLimitKeyResolver(JwtTokenValidator jwtTokenValidator) {
        return exchange -> {
            String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtTokenValidator.parse(header.substring(7));
                    if (claims.getSubject() != null && !claims.getSubject().isBlank()) {
                        return Mono.just("user:" + claims.getSubject());
                    }
                } catch (Exception ignored) {
                    // fall through to IP key
                }
            }
            String ip = "unknown";
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just("ip:" + ip);
        };
    }
}
