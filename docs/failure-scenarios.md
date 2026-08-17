# Failure scenarios and runbooks

Operators and implementers use this file. Every scenario has a **detection** signal and a **required system behavior** already implemented.

---

## 1. Payment succeeds, inventory fails

**Sequence:** OrderCreated → PaymentCompleted → InventoryReservationRequested → InventoryReservationFailed

**Behavior:**

1. Order: `PAID` → `INVENTORY_PENDING` → `INVENTORY_FAILED` → `COMPENSATION_REQUIRED`
2. Order outbox: `RefundRequested`
3. Payment: refund sandbox payment → `PaymentRefunded`
4. Order: `REFUND_PENDING` → `CANCELLED`
5. Customer notification: payment received, then unable to fulfill, then refunded

**Detection:** Grafana `fulfillx.inventory.reservation.failures`, analytics `ordersFailed`, alert `INVENTORY_FAIL_AFTER_PAY`

**Do not:** leave the order in `PAID` forever.

---

## 2. Kafka broker unavailable

Outbox `published_at` stays null. Publisher retries. Orders still persist.

**Detection:** `fulfillx.outbox.pending` gauge, Kafka exporter down.

**Recovery:** restore Kafka; publisher drains automatically. Do not manually insert duplicate events.

---

## 3. Duplicate event delivery

Kafka at-least-once. Consumer inserts `processed_events.event_id` PK. Second delivery: skip.

**Test:** publish the same `eventId` twice; assert one payment row.

---

## 4. Inventory reservation race

Two orders, 5 available, qty 4 and 3. Conditional `UPDATE ... WHERE available >= qty`. Exactly one reservation ACTIVE.

**Test:** `InventoryReservationConcurrencyTest` with Testcontainers, 20 parallel threads.

---

## 5. Reservation expires

Scheduler `ReservationExpiryJob` every 30s. `ACTIVE` and `expires_at < now()` → restore reserved qty, status `EXPIRED`, event `InventoryReleased` reason `EXPIRED`.

Order Service on `InventoryReleased` + order still `RESERVED` or `INVENTORY_PENDING`: treat as inventory failure / compensation.

---

## 6. Shipment service down

Order remains `PROCESSING`. No fake `SHIPPED`. When shipment returns, `ShipmentRequested` is still in Kafka or outbox.

If Order already published `ShipmentRequested` to outbox before crash: event will be consumed later; Shipment create is idempotent on `order_id UNIQUE`.

---

## 7. Poison message

After 3 failures, send to `topic.dlq`. Consumer metric `fulfillx.kafka.dlq`.

**Replay (ADMIN):** `POST /api/v1/admin/dlq/replay` is **not** in v1. Documented stretch. v1: dump DLQ with kafka console consumer; republish after fix.

---

## 8. Catalog unavailable at order create

Feign circuit open → 503 `CATALOG_UNAVAILABLE`. No order row. Safe.

---

## 9. Token reuse

Refresh token already revoked used again → revoke entire `family_id`. 401. Log security warning.

---

## 10. Partial basket unfulfillable

Reservation is **all-or-nothing** for the order. If any SKU cannot be placed in the **same** selected warehouse, try next warehouse that can fulfill **the entire basket**. Split-shipments are a v2 non-goal.

---

## Stuck-order detector

Analytics scheduler: any order in a non-terminal status with `updated_at` older than 30 minutes → alert `STUCK_ORDER`.
