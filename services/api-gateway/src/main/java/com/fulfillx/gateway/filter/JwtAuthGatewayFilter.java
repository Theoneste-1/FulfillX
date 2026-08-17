package com.fulfillx.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfillx.gateway.jwt.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthGatewayFilter implements GlobalFilter, Ordered {
    private static final PathPatternParser PARSER = new PathPatternParser();
    private static final List<PathPattern> PUBLIC_GET = List.of(
            PARSER.parse("/api/v1/products"),
            PARSER.parse("/api/v1/products/{id}"),
            PARSER.parse("/actuator/health"),
            PARSER.parse("/actuator/health/**"),
            PARSER.parse("/actuator/info"),
            PARSER.parse("/actuator/prometheus")
    );
    private static final List<PathPattern> PUBLIC_AUTH = List.of(
            PARSER.parse("/api/v1/auth/register"),
            PARSER.parse("/api/v1/auth/login"),
            PARSER.parse("/api/v1/auth/refresh")
    );

    private final JwtTokenValidator jwtTokenValidator;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redis;

    public JwtAuthGatewayFilter(
            JwtTokenValidator jwtTokenValidator,
            ObjectMapper objectMapper,
            ReactiveStringRedisTemplate redis
    ) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.objectMapper = objectMapper;
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isPublic(exchange)) {
            return chain.filter(exchange);
        }
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange, "Authentication required");
        }
        String token = header.substring(7);
        Claims claims;
        try {
            claims = jwtTokenValidator.parse(token);
        } catch (JwtException | IllegalArgumentException ex) {
            return unauthorized(exchange, "Authentication required");
        }
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            return chain.filter(exchange);
        }
        return redis.hasKey("auth:denylist:" + jti)
                .onErrorReturn(false)
                .flatMap(denied -> {
                    if (Boolean.TRUE.equals(denied)) {
                        return unauthorized(exchange, "Authentication required");
                    }
                    return chain.filter(exchange);
                });
    }

    private boolean isPublic(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }
        PathContainer path = PathContainer.parsePath(exchange.getRequest().getPath().value());
        if (HttpMethod.GET.equals(method) && matches(PUBLIC_GET, path)) {
            return true;
        }
        return matches(PUBLIC_AUTH, path);
    }

    private static boolean matches(List<PathPattern> patterns, PathContainer path) {
        for (PathPattern pattern : patterns) {
            if (pattern.matches(path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("code", "UNAUTHENTICATED");
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());
        body.put("correlationId", exchange.getRequest().getHeaders().getFirst(CorrelationIdGatewayFilter.CORRELATION_ID));
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":\"UNAUTHENTICATED\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
