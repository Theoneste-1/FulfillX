package com.fulfillx.order.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fulfillx.common.event.DomainEvent;
import com.fulfillx.common.json.Jsons;
import com.fulfillx.order.domain.Order;
import com.fulfillx.order.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderEventService {
    private static final Logger log = LoggerFactory.getLogger(OrderEventService.class);
    private final OrderService orderService;

    public OrderEventService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Transactional
    public void onPaymentCompleted(DomainEvent event) {
        PaymentCompletedPayload payload = Jsons.mapper().convertValue(event.payload(), PaymentCompletedPayload.class);
        Order order = orderService.requireOrder(payload.orderId());
        order.assignPayment(payload.paymentId());
        String causation = event.eventId().toString();
        if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            orderService.transition(order, OrderStatus.PAID, "PaymentCompleted", causation, true);
            orderService.transition(order, OrderStatus.INVENTORY_PENDING, "PaymentCompleted", causation, true);
            orderService.publishInventoryReservationRequested(order, causation);
            return;
        }
        if (order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.COMPENSATION_REQUIRED
                || order.getStatus() == OrderStatus.REFUND_PENDING) {
            log.info("PaymentCompleted for order {} in {}, requesting refund", order.getId(), order.getStatus());
            orderService.requestRefund(order, "Order already cancelled or compensating", causation);
            return;
        }
        log.info("Ignoring PaymentCompleted for order {} in status {}", order.getId(), order.getStatus());
    }

    @Transactional
    public void onPaymentFailed(DomainEvent event) {
        PaymentFailedPayload payload = Jsons.mapper().convertValue(event.payload(), PaymentFailedPayload.class);
        Order order = orderService.requireOrder(payload.orderId());
        if (payload.paymentId() != null) {
            order.assignPayment(payload.paymentId());
        }
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            log.info("Ignoring PaymentFailed for order {} in status {}", order.getId(), order.getStatus());
            return;
        }
        orderService.transition(order, OrderStatus.PAYMENT_FAILED, "PaymentFailed", event.eventId().toString(), true);
    }

    @Transactional
    public void onPaymentRefunded(DomainEvent event) {
        PaymentRefundedPayload payload = Jsons.mapper().convertValue(event.payload(), PaymentRefundedPayload.class);
        Order order = orderService.requireOrder(payload.orderId());
        String causation = event.eventId().toString();
        if (order.getStatus() == OrderStatus.COMPENSATION_REQUIRED) {
            orderService.transition(order, OrderStatus.REFUND_PENDING, "PaymentRefunded", causation, true);
            orderService.transition(order, OrderStatus.CANCELLED, "PaymentRefunded", causation, true);
            return;
        }
        if (order.getStatus() == OrderStatus.REFUND_PENDING) {
            orderService.transition(order, OrderStatus.CANCELLED, "PaymentRefunded", causation, true);
            return;
        }
        log.info("Ignoring PaymentRefunded for order {} in status {}", order.getId(), order.getStatus());
    }

    @Transactional
    public void onInventoryReserved(DomainEvent event) {
        InventoryReservedPayload payload = Jsons.mapper().convertValue(event.payload(), InventoryReservedPayload.class);
        Order order = orderService.requireOrder(payload.orderId());
        if (order.getStatus() != OrderStatus.INVENTORY_PENDING) {
            log.info("Ignoring InventoryReserved for order {} in status {}", order.getId(), order.getStatus());
            return;
        }
        order.assignWarehouse(payload.warehouseId());
        String causation = event.eventId().toString();
        orderService.transition(order, OrderStatus.RESERVED, "InventoryReserved", causation, true);
        orderService.transition(order, OrderStatus.PROCESSING, "InventoryReserved", causation, true);
        orderService.publishShipmentRequested(order, causation);
    }

    @Transactional
    public void onInventoryReservationFailed(DomainEvent event) {
        InventoryReservationFailedPayload payload =
                Jsons.mapper().convertValue(event.payload(), InventoryReservationFailedPayload.class);
        Order order = orderService.requireOrder(payload.orderId());
        if (order.getStatus() != OrderStatus.INVENTORY_PENDING) {
            log.info("Ignoring InventoryReservationFailed for order {} in status {}", order.getId(), order.getStatus());
            return;
        }
        String causation = event.eventId().toString();
        String reason = payload.reason() == null ? "INVENTORY_FAILED" : payload.reason();
        orderService.transition(order, OrderStatus.INVENTORY_FAILED, reason, causation, true);
        orderService.transition(order, OrderStatus.COMPENSATION_REQUIRED, reason, causation, true);
        orderService.requestRefund(order, reason, causation);
    }

    @Transactional
    public void onInventoryReleased(DomainEvent event) {
        InventoryReleasedPayload payload = Jsons.mapper().convertValue(event.payload(), InventoryReleasedPayload.class);
        if (!"EXPIRED".equals(payload.reason())) {
            return;
        }
        Order order = orderService.requireOrder(payload.orderId());
        String causation = event.eventId().toString();
        if (order.getStatus() == OrderStatus.INVENTORY_PENDING) {
            orderService.transition(order, OrderStatus.INVENTORY_FAILED, "EXPIRED", causation, true);
            orderService.transition(order, OrderStatus.COMPENSATION_REQUIRED, "EXPIRED", causation, true);
            orderService.requestRefund(order, "EXPIRED", causation);
            return;
        }
        if (order.getStatus() == OrderStatus.RESERVED || order.getStatus() == OrderStatus.PROCESSING) {
            orderService.transition(order, OrderStatus.CANCELLED, "Reservation expired", causation, true);
            orderService.requestRefund(order, "EXPIRED", causation);
            return;
        }
        log.info("Ignoring expired InventoryReleased for order {} in status {}", order.getId(), order.getStatus());
    }

    @Transactional
    public void onShipmentCreated(DomainEvent event) {
        ShipmentCreatedPayload payload = Jsons.mapper().convertValue(event.payload(), ShipmentCreatedPayload.class);
        Order order = orderService.requireOrder(payload.orderId());
        if (order.getStatus() != OrderStatus.PROCESSING) {
            log.info("Ignoring ShipmentCreated for order {} in status {}", order.getId(), order.getStatus());
            return;
        }
        orderService.transition(order, OrderStatus.SHIPPED, "ShipmentCreated", event.eventId().toString(), true);
    }

    @Transactional
    public void onShipmentDelivered(DomainEvent event) {
        ShipmentDeliveredPayload payload = Jsons.mapper().convertValue(event.payload(), ShipmentDeliveredPayload.class);
        Order order = orderService.requireOrder(payload.orderId());
        if (order.getStatus() != OrderStatus.SHIPPED) {
            log.info("Ignoring ShipmentDelivered for order {} in status {}", order.getId(), order.getStatus());
            return;
        }
        orderService.transition(order, OrderStatus.DELIVERED, "ShipmentDelivered", event.eventId().toString(), true);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentCompletedPayload(UUID paymentId, UUID orderId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentFailedPayload(UUID paymentId, UUID orderId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentRefundedPayload(UUID paymentId, UUID orderId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InventoryReservedPayload(UUID reservationId, UUID orderId, UUID warehouseId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InventoryReservationFailedPayload(UUID orderId, String reason, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InventoryReleasedPayload(UUID reservationId, UUID orderId, UUID warehouseId, String reason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShipmentCreatedPayload(UUID shipmentId, UUID orderId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ShipmentDeliveredPayload(UUID shipmentId, UUID orderId) {
    }
}
