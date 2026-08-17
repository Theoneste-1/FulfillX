package com.fulfillx.order.api;

import com.fulfillx.common.api.PageResponse;
import com.fulfillx.common.web.Headers;
import com.fulfillx.order.application.OrderService;
import com.fulfillx.order.domain.OrderStatus;
import com.fulfillx.security.FulfillxPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<OrderResponse> create(
            @RequestHeader(value = Headers.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal FulfillxPrincipal principal
    ) {
        OrderResponse body = orderService.create(idempotencyKey, customerId, request, principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public OrderResponse get(
            @PathVariable UUID id,
            @AuthenticationPrincipal FulfillxPrincipal principal
    ) {
        return orderService.get(id, principal);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal FulfillxPrincipal principal
    ) {
        return PageResponse.of(orderService.list(status, customerId, from, to, pageable, principal));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public OrderResponse cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal FulfillxPrincipal principal
    ) {
        return orderService.cancel(id, request, principal);
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("isAuthenticated()")
    public TimelineResponse timeline(
            @PathVariable UUID id,
            @AuthenticationPrincipal FulfillxPrincipal principal
    ) {
        return orderService.timeline(id, principal);
    }
}
