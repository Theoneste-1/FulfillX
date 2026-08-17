# Event contracts

Topics and envelope are defined in `docs/product-spec.md`. This file is the payload schema. `schemaVersion` starts at `1`. Additive optional fields are allowed; renaming/removing requires a new version and dual consumers.

Jackson property names: camelCase.

---

## orders.events

### OrderCreated

```json
{
  "orderId": "uuid",
  "orderNumber": "ORD-20260817-K4H91Q",
  "customerId": "uuid",
  "customerEmail": "ada@example.com",
  "status": "PAYMENT_PENDING",
  "totalAmount": 99.98,
  "currency": "USD",
  "shippingAddress": {
    "line1": "12 KN Street",
    "line2": null,
    "city": "Kigali",
    "region": "Kigali",
    "postalCode": "00000",
    "country": "RW"
  },
  "items": [
    {
      "productId": "uuid",
      "sku": "FX-MOUSE-01",
      "name": "FulfillX Vertical Mouse",
      "quantity": 2,
      "unitPrice": 49.99
    }
  ],
  "simulation": { "paymentFail": false }
}
```

### InventoryReservationRequested

Published when order enters `INVENTORY_PENDING`.

```json
{
  "orderId": "uuid",
  "customerId": "uuid",
  "destinationCountry": "RW",
  "items": [
    { "productId": "uuid", "sku": "FX-MOUSE-01", "quantity": 2 }
  ]
}
```

### OrderStatusChanged

```json
{
  "orderId": "uuid",
  "orderNumber": "ORD-...",
  "oldStatus": "PAID",
  "newStatus": "INVENTORY_PENDING",
  "reason": "PaymentCompleted",
  "customerId": "uuid"
}
```

### OrderCancelled

```json
{
  "orderId": "uuid",
  "reason": "Customer requested",
  "previousStatus": "RESERVED",
  "refundRequired": true
}
```

### RefundRequested

```json
{
  "orderId": "uuid",
  "paymentId": "uuid",
  "amount": 99.98,
  "currency": "USD",
  "reason": "INVENTORY_FAILED"
}
```

---

## payments.events

### PaymentCompleted

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": 99.98,
  "currency": "USD",
  "provider": "SANDBOX",
  "providerReference": "pay_sandbox_..."
}
```

### PaymentFailed

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": 99.98,
  "currency": "USD",
  "reason": "SIMULATED_DECLINE"
}
```

### PaymentRefunded

```json
{
  "paymentId": "uuid",
  "orderId": "uuid",
  "amount": 99.98,
  "currency": "USD"
}
```

---

## inventory.events

### InventoryReserved

```json
{
  "reservationId": "uuid",
  "orderId": "uuid",
  "warehouseId": "uuid",
  "warehouseCode": "KGL-01",
  "items": [
    { "productId": "uuid", "sku": "FX-MOUSE-01", "quantity": 2 }
  ]
}
```

### InventoryReservationFailed

```json
{
  "orderId": "uuid",
  "reason": "INSUFFICIENT_INVENTORY",
  "message": "No warehouse could fulfill the basket"
}
```

### InventoryReleased

```json
{
  "reservationId": "uuid",
  "orderId": "uuid",
  "warehouseId": "uuid",
  "reason": "EXPIRED | CANCELLED | MANUAL | COMPENSATION"
}
```

### InventoryStockChanged

```json
{
  "warehouseId": "uuid",
  "productId": "uuid",
  "sku": "FX-MOUSE-01",
  "available": 8,
  "quantityOnHand": 20,
  "quantityReserved": 12
}
```

---

## warehouses.events

### WarehouseUpserted

Full snapshot (created or updated).

```json
{
  "warehouseId": "uuid",
  "code": "KGL-01",
  "name": "Kigali Hub",
  "country": "RW",
  "city": "Kigali",
  "latitude": -1.9441,
  "longitude": 30.0619,
  "capacity": 500,
  "currentWorkload": 410,
  "status": "OPERATIONAL",
  "supportedRegions": ["RW", "KE", "UG"]
}
```

---

## shipments.events

### ShipmentCreated

```json
{
  "shipmentId": "uuid",
  "orderId": "uuid",
  "warehouseId": "uuid",
  "carrier": "FXL",
  "trackingNumber": "FX-FXL-AB12CD34EF56",
  "status": "CREATED",
  "estimatedDelivery": "2026-08-20T12:00:00Z"
}
```

### ShipmentStatusChanged

```json
{
  "shipmentId": "uuid",
  "orderId": "uuid",
  "oldStatus": "IN_TRANSIT",
  "newStatus": "OUT_FOR_DELIVERY",
  "location": "Kigali",
  "description": "On vehicle"
}
```

### ShipmentDelayed

```json
{
  "shipmentId": "uuid",
  "orderId": "uuid",
  "estimatedDelivery": "2026-08-16T12:00:00Z",
  "status": "IN_TRANSIT"
}
```

### ShipmentDelivered

```json
{
  "shipmentId": "uuid",
  "orderId": "uuid",
  "actualDelivery": "2026-08-17T09:11:00Z"
}
```

---

## notifications.events

### NotificationDispatched

```json
{
  "notificationId": "uuid",
  "orderId": "uuid",
  "channel": "EMAIL",
  "template": "ORDER_SHIPPED",
  "recipient": "ada@example.com",
  "status": "SENT"
}
```

---

## Consumer mapping (must implement)

| Event | payment | inventory | warehouse | shipment | order | notification | analytics |
|---|---|---|---|---|---|---|---|
| OrderCreated | process payment | — | — | — | — | email | metrics |
| InventoryReservationRequested | — | reserve | — | — | — | — | — |
| PaymentCompleted | — | — | — | — | → PAID → INVENTORY_PENDING + request reservation | email | metrics |
| PaymentFailed | — | — | — | — | → PAYMENT_FAILED | email | metrics |
| PaymentRefunded | — | — | — | — | → CANCELLED from REFUND_PENDING | email | metrics |
| InventoryReserved | — | — | workload++ | create shipment after PROCESSING; order will go RESERVED then PROCESSING | → RESERVED → PROCESSING | email | metrics |
| InventoryReservationFailed | — | — | — | — | compensation | email | metrics |
| InventoryReleased | — | — | workload-- | — | if needed | — | metrics |
| ShipmentCreated | — | — | — | — | → SHIPPED | email | metrics |
| ShipmentDelivered | — | convert reserved to shipped decrement on_hand | workload-- | — | → DELIVERED | email | metrics |
| ShipmentDelayed | — | — | — | — | — | email | metrics |
| OrderCancelled | refund if paid | release | — | cancel if not shipped | — | email | metrics |

**Shipment creation trigger:** Shipment Service consumes `InventoryReserved` **and** waits until it also observed `OrderStatusChanged` to `PROCESSING`, **or** simpler v1: Order Service, upon entering `PROCESSING`, publishes `ShipmentRequested` on `orders.events`.

### ShipmentRequested (orders.events)

```json
{
  "orderId": "uuid",
  "warehouseId": "uuid",
  "destinationCountry": "RW",
  "items": [{ "productId": "uuid", "sku": "FX-MOUSE-01", "quantity": 2 }]
}
```

Implement this. It avoids a distributed “and” join in Shipment Service.
