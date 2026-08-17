package com.fulfillx.notification.domain;

import com.fulfillx.common.event.EventTypes;

import java.util.Map;

public final class NotificationTemplates {
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String INVENTORY_RESERVED = "INVENTORY_RESERVED";
    public static final String FULFILLMENT_FAILED = "FULFILLMENT_FAILED";
    public static final String ORDER_SHIPPED = "ORDER_SHIPPED";
    public static final String ORDER_DELIVERED = "ORDER_DELIVERED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String SHIPMENT_DELAYED = "SHIPMENT_DELAYED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    private static final Map<String, String> EVENT_TO_TEMPLATE = Map.ofEntries(
            Map.entry(EventTypes.ORDER_CREATED, ORDER_CREATED),
            Map.entry(EventTypes.PAYMENT_COMPLETED, PAYMENT_COMPLETED),
            Map.entry(EventTypes.PAYMENT_FAILED, PAYMENT_FAILED),
            Map.entry(EventTypes.INVENTORY_RESERVED, INVENTORY_RESERVED),
            Map.entry(EventTypes.INVENTORY_RESERVATION_FAILED, FULFILLMENT_FAILED),
            Map.entry(EventTypes.SHIPMENT_CREATED, ORDER_SHIPPED),
            Map.entry(EventTypes.SHIPMENT_DELIVERED, ORDER_DELIVERED),
            Map.entry(EventTypes.ORDER_CANCELLED, ORDER_CANCELLED),
            Map.entry(EventTypes.SHIPMENT_DELAYED, SHIPMENT_DELAYED),
            Map.entry(EventTypes.PAYMENT_REFUNDED, PAYMENT_REFUNDED)
    );

    private NotificationTemplates() {
    }

    public static String forEventType(String eventType) {
        return EVENT_TO_TEMPLATE.get(eventType);
    }

    public static String subject(String template) {
        return switch (template) {
            case ORDER_CREATED -> "Your FulfillX order was received";
            case PAYMENT_COMPLETED -> "Payment received";
            case PAYMENT_FAILED -> "Payment failed";
            case INVENTORY_RESERVED -> "Inventory reserved for your order";
            case FULFILLMENT_FAILED -> "We could not fulfill your order";
            case ORDER_SHIPPED -> "Your order has shipped";
            case ORDER_DELIVERED -> "Your order was delivered";
            case ORDER_CANCELLED -> "Your order was cancelled";
            case SHIPMENT_DELAYED -> "Your shipment is delayed";
            case PAYMENT_REFUNDED -> "A refund was issued";
            default -> "FulfillX notification";
        };
    }

    public static String body(String template, Map<String, Object> payload) {
        String orderNumber = text(payload, "orderNumber");
        String orderId = text(payload, "orderId");
        String tracking = text(payload, "trackingNumber");
        String reason = text(payload, "reason");
        String ref = orderNumber != null ? orderNumber : orderId;
        return switch (template) {
            case ORDER_CREATED -> "Your order " + nvl(ref) + " has been created and is awaiting payment.";
            case PAYMENT_COMPLETED -> "Payment for order " + nvl(ref) + " was completed.";
            case PAYMENT_FAILED -> "Payment for order " + nvl(ref) + " failed"
                    + (reason != null ? ": " + reason : "") + ".";
            case INVENTORY_RESERVED -> "Inventory was reserved for order " + nvl(ref) + ".";
            case FULFILLMENT_FAILED -> "Fulfillment failed for order " + nvl(ref)
                    + (reason != null ? ": " + reason : "") + ".";
            case ORDER_SHIPPED -> "Order " + nvl(ref) + " has shipped"
                    + (tracking != null ? ". Tracking number: " + tracking : "") + ".";
            case ORDER_DELIVERED -> "Order " + nvl(ref) + " was delivered.";
            case ORDER_CANCELLED -> "Order " + nvl(ref) + " was cancelled"
                    + (reason != null ? ": " + reason : "") + ".";
            case SHIPMENT_DELAYED -> "Shipment for order " + nvl(ref) + " is delayed.";
            case PAYMENT_REFUNDED -> "A refund was issued for order " + nvl(ref) + ".";
            default -> "Update for order " + nvl(ref) + ".";
        };
    }

    private static String text(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() || "null".equals(text) ? null : text;
    }

    private static String nvl(String value) {
        return value == null ? "(unknown)" : value;
    }
}
