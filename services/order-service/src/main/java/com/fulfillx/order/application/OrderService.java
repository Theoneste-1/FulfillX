package com.fulfillx.order.application;

import com.fulfillx.common.error.ErrorCode;
import com.fulfillx.common.error.FulfillxException;
import com.fulfillx.common.event.EventTypes;
import com.fulfillx.common.json.Jsons;
import com.fulfillx.common.observability.FulfillxMetrics;
import com.fulfillx.common.security.Roles;
import com.fulfillx.order.api.CancelOrderRequest;
import com.fulfillx.order.api.CreateOrderRequest;
import com.fulfillx.order.api.OrderResponse;
import com.fulfillx.order.api.TimelineResponse;
import com.fulfillx.order.domain.IdempotencyKey;
import com.fulfillx.order.domain.Order;
import com.fulfillx.order.domain.OrderItem;
import com.fulfillx.order.domain.OrderStatus;
import com.fulfillx.order.domain.OrderStatusHistory;
import com.fulfillx.order.infrastructure.client.CatalogGateway;
import com.fulfillx.order.infrastructure.client.CatalogProduct;
import com.fulfillx.order.infrastructure.persistence.IdempotencyKeyRepository;
import com.fulfillx.order.infrastructure.persistence.OrderRepository;
import com.fulfillx.outbox.OutboxWriter;
import com.fulfillx.security.FulfillxPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final OrderRepository orders;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final CatalogGateway catalogGateway;
    private final OutboxWriter outboxWriter;
    private final FulfillxMetrics metrics;

    public OrderService(
            OrderRepository orders,
            IdempotencyKeyRepository idempotencyKeys,
            CatalogGateway catalogGateway,
            OutboxWriter outboxWriter,
            FulfillxMetrics metrics
    ) {
        this.orders = orders;
        this.idempotencyKeys = idempotencyKeys;
        this.catalogGateway = catalogGateway;
        this.outboxWriter = outboxWriter;
        this.metrics = metrics;
    }

    @Transactional
    public OrderResponse create(
            String idempotencyKey,
            UUID customerIdQuery,
            CreateOrderRequest request,
            FulfillxPrincipal principal
    ) {
        OrderAccess.requireIdempotencyKey(idempotencyKey);
        UUID customerId = OrderAccess.customerIdForCreate(principal, request.customerId(), customerIdQuery);
        String requestHash = sha256(customerId + ":" + Jsons.write(request));

        IdempotencyKey existing = idempotencyKeys.findByActorIdAndKey(principal.userId(), idempotencyKey).orElse(null);
        if (existing != null && !expired(existing)) {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new FulfillxException(
                        ErrorCode.IDEMPOTENCY_KEY_REUSED,
                        "Idempotency-Key was already used with a different payload"
                );
            }
            return Jsons.read(existing.getResponseBody(), OrderResponse.class);
        }

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        String currency = request.currency() == null || request.currency().isBlank() ? "USD" : request.currency();
        for (CreateOrderRequest.OrderItemRequest line : request.items()) {
            CatalogProduct product = catalogGateway.requireActiveProduct(line.productId());
            BigDecimal unitPrice = product.price().setScale(2, RoundingMode.HALF_UP);
            items.add(OrderItem.snapshot(product.id(), product.sku(), product.name(), line.quantity(), unitPrice));
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(line.quantity())).setScale(2, RoundingMode.HALF_UP));
        }

        CreateOrderRequest.ShippingAddressRequest shipping = request.shippingAddress();
        Order order = Order.create(
                uniqueOrderNumber(),
                customerId,
                principal.email(),
                total,
                currency,
                shipping.line1(),
                shipping.line2(),
                shipping.city(),
                shipping.region(),
                shipping.postalCode(),
                shipping.country().toUpperCase()
        );
        items.forEach(order::addItem);
        order.transitionTo(OrderStatus.PAYMENT_PENDING, "Submitted for payment");
        orders.save(order);

        boolean paymentFail = request.simulation() != null && request.simulation().enabled();
        outboxWriter.enqueue(
                EventTypes.ORDER_CREATED,
                "Order",
                order.getId().toString(),
                orderCreatedPayload(order, paymentFail),
                null
        );

        OrderResponse response = OrderResponse.from(order);
        persistIdempotency(existing, principal.userId(), idempotencyKey, requestHash, response);
        metrics.increment("fulfillx.orders.created");
        return response;
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id, FulfillxPrincipal principal) {
        Order order = loadWithItems(id);
        OrderAccess.requireRead(order, principal);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(
            OrderStatus status,
            UUID customerId,
            Instant from,
            Instant to,
            Pageable pageable,
            FulfillxPrincipal principal
    ) {
        UUID effectiveCustomer = customerId;
        if (!OrderAccess.hasAnyRole(principal, Roles.SUPPORT, Roles.ADMIN, Roles.LOGISTICS_OPERATOR)) {
            effectiveCustomer = principal.userId();
        }
        return orders.search(effectiveCustomer, status, from, to, pageable).map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse cancel(UUID id, CancelOrderRequest request, FulfillxPrincipal principal) {
        Order order = loadWithItems(id);
        OrderAccess.requireCancel(order, principal);
        if (!OrderStateMachine.canCancel(order.getStatus())) {
            throw new FulfillxException(
                    ErrorCode.ORDER_NOT_CANCELLABLE,
                    "Order " + order.getOrderNumber() + " cannot be cancelled from " + order.getStatus()
            );
        }
        order.setCancellationReason(request.reason());
        OrderStatus previous = order.getStatus();
        boolean refundRequired = previous == OrderStatus.PAID
                || previous == OrderStatus.RESERVED
                || previous == OrderStatus.PROCESSING;
        if (previous == OrderStatus.PAID) {
            transition(order, OrderStatus.COMPENSATION_REQUIRED, request.reason(), null, true);
            requestRefund(order, request.reason(), null);
        } else {
            transition(order, OrderStatus.CANCELLED, request.reason(), null, true);
            if (refundRequired) {
                requestRefund(order, request.reason(), null);
            }
        }
        outboxWriter.enqueue(
                EventTypes.ORDER_CANCELLED,
                "Order",
                order.getId().toString(),
                Map.of(
                        "orderId", order.getId(),
                        "reason", request.reason(),
                        "previousStatus", previous.name(),
                        "refundRequired", refundRequired
                ),
                null
        );
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public TimelineResponse timeline(UUID id, FulfillxPrincipal principal) {
        Order order = orders.findWithDetailsById(id)
                .orElseThrow(() -> new FulfillxException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + id));
        OrderAccess.requireRead(order, principal);
        List<TimelineResponse.TimelineEntry> entries = order.getStatusHistory().stream()
                .map(this::toTimelineEntry)
                .toList();
        return new TimelineResponse(entries);
    }

    public void transition(Order order, OrderStatus target, String reason, String causationId, boolean publishChanged) {
        OrderStatus oldStatus = order.getStatus();
        order.transitionTo(target, reason);
        if (publishChanged) {
            publishStatusChanged(order, oldStatus, target, reason, causationId);
        }
        if (target == OrderStatus.DELIVERED) {
            metrics.increment("fulfillx.orders.completed");
        }
        if (target == OrderStatus.PAYMENT_FAILED || target == OrderStatus.FAILED || target == OrderStatus.INVENTORY_FAILED) {
            metrics.increment("fulfillx.orders.failed");
        }
    }

    public void publishStatusChanged(
            Order order,
            OrderStatus oldStatus,
            OrderStatus newStatus,
            String reason,
            String causationId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("oldStatus", oldStatus == null ? null : oldStatus.name());
        payload.put("newStatus", newStatus.name());
        payload.put("reason", reason);
        payload.put("customerId", order.getCustomerId());
        outboxWriter.enqueue(
                EventTypes.ORDER_STATUS_CHANGED,
                "Order",
                order.getId().toString(),
                payload,
                causationId
        );
    }

    public void requestRefund(Order order, String reason, String causationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("paymentId", order.getPaymentId());
        payload.put("amount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("reason", reason);
        outboxWriter.enqueue(
                EventTypes.REFUND_REQUESTED,
                "Order",
                order.getId().toString(),
                payload,
                causationId
        );
    }

    public void publishInventoryReservationRequested(Order order, String causationId) {
        List<Map<String, Object>> items = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", item.getProductId());
                    row.put("sku", item.getSku());
                    row.put("quantity", item.getQuantity());
                    return row;
                })
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("customerId", order.getCustomerId());
        payload.put("destinationCountry", order.getShippingCountry());
        payload.put("items", items);
        outboxWriter.enqueue(
                EventTypes.INVENTORY_RESERVATION_REQUESTED,
                "Order",
                order.getId().toString(),
                payload,
                causationId
        );
    }

    public void publishShipmentRequested(Order order, String causationId) {
        List<Map<String, Object>> items = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", item.getProductId());
                    row.put("sku", item.getSku());
                    row.put("quantity", item.getQuantity());
                    return row;
                })
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("warehouseId", order.getWarehouseId());
        payload.put("destinationCountry", order.getShippingCountry());
        payload.put("items", items);
        outboxWriter.enqueue(
                EventTypes.SHIPMENT_REQUESTED,
                "Order",
                order.getId().toString(),
                payload,
                causationId
        );
    }

    public Order requireOrder(UUID orderId) {
        return orders.findWithItemsById(orderId)
                .orElseThrow(() -> new FulfillxException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId));
    }

    private Order loadWithItems(UUID id) {
        return orders.findWithItemsById(id)
                .orElseThrow(() -> new FulfillxException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + id));
    }

    private TimelineResponse.TimelineEntry toTimelineEntry(OrderStatusHistory history) {
        return new TimelineResponse.TimelineEntry(
                history.getChangedAt(),
                "ORDER_STATUS",
                history.getOldStatus() == null ? null : history.getOldStatus().name(),
                history.getNewStatus().name(),
                history.getReason()
        );
    }

    private Map<String, Object> orderCreatedPayload(Order order, boolean paymentFail) {
        List<Map<String, Object>> items = order.getItems().stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", item.getProductId());
                    row.put("sku", item.getSku());
                    row.put("name", item.getName());
                    row.put("quantity", item.getQuantity());
                    row.put("unitPrice", item.getUnitPrice());
                    return row;
                })
                .toList();
        Map<String, Object> shipping = new LinkedHashMap<>();
        shipping.put("line1", order.getShippingLine1());
        shipping.put("line2", order.getShippingLine2());
        shipping.put("city", order.getShippingCity());
        shipping.put("region", order.getShippingRegion());
        shipping.put("postalCode", order.getShippingPostal());
        shipping.put("country", order.getShippingCountry());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.getId());
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("customerId", order.getCustomerId());
        payload.put("customerEmail", order.getCustomerEmail());
        payload.put("status", order.getStatus().name());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("shippingAddress", shipping);
        payload.put("items", items);
        payload.put("simulation", Map.of("paymentFail", paymentFail));
        return payload;
    }

    private void persistIdempotency(
            IdempotencyKey existing,
            UUID actorId,
            String key,
            String requestHash,
            OrderResponse response
    ) {
        String body = Jsons.write(response);
        if (existing != null) {
            existing.replace(requestHash, HttpStatus.ACCEPTED.value(), body);
            return;
        }
        try {
            idempotencyKeys.saveAndFlush(IdempotencyKey.store(
                    actorId,
                    key,
                    requestHash,
                    HttpStatus.ACCEPTED.value(),
                    body
            ));
        } catch (DataIntegrityViolationException ex) {
            IdempotencyKey raced = idempotencyKeys.findByActorIdAndKey(actorId, key)
                    .orElseThrow(() -> ex);
            if (!raced.getRequestHash().equals(requestHash)) {
                throw new FulfillxException(
                        ErrorCode.IDEMPOTENCY_KEY_REUSED,
                        "Idempotency-Key was already used with a different payload"
                );
            }
        }
    }

    private boolean expired(IdempotencyKey key) {
        return key.getCreatedAt().plus(IDEMPOTENCY_TTL).isBefore(Instant.now());
    }

    private String uniqueOrderNumber() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String candidate = OrderNumberGenerator.next();
            if (!orders.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new FulfillxException(ErrorCode.CONFLICT, "Could not allocate a unique order number");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
