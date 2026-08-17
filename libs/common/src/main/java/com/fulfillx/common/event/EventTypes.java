package com.fulfillx.common.event;

public final class EventTypes {
    public static final String ORDER_CREATED = "OrderCreated";
    public static final String ORDER_STATUS_CHANGED = "OrderStatusChanged";
    public static final String ORDER_CANCELLED = "OrderCancelled";
    public static final String INVENTORY_RESERVATION_REQUESTED = "InventoryReservationRequested";
    public static final String SHIPMENT_REQUESTED = "ShipmentRequested";
    public static final String REFUND_REQUESTED = "RefundRequested";

    public static final String PAYMENT_COMPLETED = "PaymentCompleted";
    public static final String PAYMENT_FAILED = "PaymentFailed";
    public static final String PAYMENT_REFUNDED = "PaymentRefunded";

    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_RESERVATION_FAILED = "InventoryReservationFailed";
    public static final String INVENTORY_RELEASED = "InventoryReleased";
    public static final String INVENTORY_STOCK_CHANGED = "InventoryStockChanged";

    public static final String WAREHOUSE_UPSERTED = "WarehouseUpserted";

    public static final String SHIPMENT_CREATED = "ShipmentCreated";
    public static final String SHIPMENT_STATUS_CHANGED = "ShipmentStatusChanged";
    public static final String SHIPMENT_DELAYED = "ShipmentDelayed";
    public static final String SHIPMENT_DELIVERED = "ShipmentDelivered";

    public static final String NOTIFICATION_DISPATCHED = "NotificationDispatched";

    private EventTypes() {
    }
}
