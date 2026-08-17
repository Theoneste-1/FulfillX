package com.fulfillx.common.event;

public final class Topics {
    public static final String ORDERS = "orders.events";
    public static final String PAYMENTS = "payments.events";
    public static final String INVENTORY = "inventory.events";
    public static final String WAREHOUSES = "warehouses.events";
    public static final String SHIPMENTS = "shipments.events";
    public static final String NOTIFICATIONS = "notifications.events";

    public static String dlq(String topic) {
        return topic + ".dlq";
    }

    private Topics() {
    }
}
