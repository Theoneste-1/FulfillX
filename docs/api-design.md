# API Design

Base URL (Compose): `http://localhost:8080`  
All bodies JSON. Dates ISO-8601 UTC.

OpenAPI is also served per service at `/swagger-ui.html` (not via gateway in v1 except aggregated later).

---

## Auth — `/api/v1/auth`

### POST `/register`

Roles: public. Default role CUSTOMER. Only ADMIN can create other roles via `POST /api/v1/users`.

Request:

```json
{
  "email": "ada@example.com",
  "password": "Customer123!",
  "fullName": "Ada Lovelace"
}
```

Validation: email unique, password min 8 with upper, lower, digit. 201:

```json
{
  "id": "uuid",
  "email": "ada@example.com",
  "fullName": "Ada Lovelace",
  "roles": ["CUSTOMER"],
  "status": "ACTIVE"
}
```

409 `EMAIL_TAKEN`

### POST `/login`

```json
{ "email": "ada@example.com", "password": "Customer123!" }
```

200:

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

401 `INVALID_CREDENTIALS`  
403 `ACCOUNT_DISABLED`

### POST `/refresh`

```json
{ "refreshToken": "<opaque>" }
```

Same 200 shape as login. 401 `INVALID_REFRESH_TOKEN`

### POST `/logout`

JWT required. Body: `{ "refreshToken": "<opaque>" }`. 204. Access token jti added to Redis denylist until expiry.

### GET `/me`

JWT required. 200 user profile.

---

## Catalog — `/api/v1/products`

### POST `/`  ADMIN

```json
{
  "sku": "FX-MOUSE-01",
  "name": "FulfillX Vertical Mouse",
  "description": "Ergonomic mouse",
  "category": "ELECTRONICS",
  "price": 49.99,
  "currency": "USD",
  "weightKg": 0.12,
  "active": true
}
```

201 product. 409 `SKU_TAKEN`

### GET `/{id}`  public

200 product. 404 `PRODUCT_NOT_FOUND`

### GET `/`  public

Query: `category`, `active`, `q` (name/sku contains), `page`, `size`, `sort`

### PUT `/{id}`  ADMIN

Full update. 200.

### PATCH `/{id}/status`  ADMIN

```json
{ "active": false }
```

Categories v1 (enum): `ELECTRONICS`, `HOME`, `APPAREL`, `SPORTS`, `GROCERY`, `OTHER`

---

## Warehouses — `/api/v1/warehouses`

### POST `/`  ADMIN

```json
{
  "code": "KGL-01",
  "name": "Kigali Hub",
  "country": "RW",
  "city": "Kigali",
  "latitude": -1.9441,
  "longitude": 30.0619,
  "capacity": 500,
  "supportedRegions": ["RW", "KE", "UG", "TZ", "US"],
  "status": "OPERATIONAL"
}
```

`code` unique. 201.

### GET `/`  WAREHOUSE_OPERATOR, LOGISTICS_OPERATOR, ADMIN, SUPPORT

Query: `status`, `country`

### GET `/{id}`  same roles

### PATCH `/{id}/status`  ADMIN

`OPERATIONAL | MAINTENANCE | CLOSED`

### GET `/{id}/workload`  same roles

```json
{
  "warehouseId": "uuid",
  "capacity": 500,
  "currentWorkload": 410,
  "utilization": 0.82
}
```

Workload is updated when Inventory publishes reservation/release and Shipment completes (events). Warehouse Service consumes those events to adjust `current_workload`.

---

## Inventory — `/api/v1/inventory`

### PUT `/levels`  WAREHOUSE_OPERATOR, ADMIN

Set on-hand (receiving). Does not reduce below reserved.

```json
{
  "warehouseId": "uuid",
  "productId": "uuid",
  "quantityOnHand": 100,
  "reason": "RECEIVED"
}
```

### GET `/`  WAREHOUSE_OPERATOR, ADMIN, SUPPORT

Query: `warehouseId`, `productId`, `lowStock` (available <= 10)

Response item:

```json
{
  "id": "uuid",
  "warehouseId": "uuid",
  "productId": "uuid",
  "sku": "FX-MOUSE-01",
  "quantityOnHand": 100,
  "quantityReserved": 12,
  "available": 88,
  "version": 4
}
```

### GET `/reservations/{orderId}`  ADMIN, SUPPORT, LOGISTICS_OPERATOR

### POST `/reservations/{id}/release`  ADMIN

Manual release (ops). Publishes `InventoryReleased`.

Inventory reservation from the happy path is **not** a public POST; it is triggered by `PaymentCompleted` / order moving to `INVENTORY_PENDING` via `InventoryReservationRequested` (event type on `orders.events`).

---

## Orders — `/api/v1/orders`

### POST `/`  CUSTOMER (self), ADMIN

Header `Idempotency-Key` required.

```json
{
  "items": [
    { "productId": "uuid", "quantity": 2 }
  ],
  "shippingAddress": {
    "line1": "12 KN Street",
    "line2": null,
    "city": "Kigali",
    "region": "Kigali",
    "postalCode": "00000",
    "country": "RW"
  },
  "currency": "USD",
  "simulation": {
    "paymentFail": false
  }
}
```

Server:

1. Load products from Catalog (Feign)
2. Reject inactive products `PRODUCT_INACTIVE`
3. Compute `totalAmount`
4. Insert order `CREATED` then immediately `PAYMENT_PENDING` (two history rows)
5. Outbox `OrderCreated`
6. 202 Accepted (async fulfillment) with Order body

CUSTOMER: `customerId` = JWT `sub`. ADMIN may pass `customerId` query/body only when role is ADMIN.

### GET `/{id}`  owner, SUPPORT, ADMIN, LOGISTICS_OPERATOR

Includes items + latest shipment summary if any.

### GET `/`  

CUSTOMER: own orders. Others: filter `status`, `customerId`, `from`, `to`.

### POST `/{id}/cancel`  owner (before SHIPPED), ADMIN, SUPPORT

Reason required. Triggers reservation release / refund as needed.

```json
{ "reason": "Customer requested" }
```

409 if not cancellable.

### GET `/{id}/timeline`  

Status history + shipment events merged, sorted by time.

---

## Payments — `/api/v1/payments`

### GET `/orders/{orderId}`  owner, SUPPORT, ADMIN

### POST `/orders/{orderId}/retry`  ADMIN

Only from `PAYMENT_FAILED` after order is reopened — v1: only if order is still `PAYMENT_FAILED` and ADMIN forces a new attempt (new payment row `attempt`). Prefer new order for customers.

---

## Shipments — `/api/v1/shipments`

### GET `/`  LOGISTICS_OPERATOR, ADMIN, SUPPORT

Query: `status`, `warehouseId`, `delayed=true`

### GET `/{id}`  also CUSTOMER if their order

### POST `/{id}/status`  LOGISTICS_OPERATOR, ADMIN

```json
{
  "status": "IN_TRANSIT",
  "location": "Nairobi hub",
  "description": "Departed origin"
}
```

Writes `shipment_events`, publishes `ShipmentStatusChanged` / `OrderShipped` / `OrderDelivered` as appropriate.

---

## Notifications — `/api/v1/notifications`

### GET `/`  ADMIN, SUPPORT

Query: `orderId`, `channel`, `status`

CUSTOMER: `GET /api/v1/notifications/me`

---

## Analytics — `/api/v1/analytics`

All ADMIN, LOGISTICS_OPERATOR, SUPPORT, WAREHOUSE_OPERATOR (read).

### GET `/overview`

```json
{
  "ordersTotal": 12842,
  "ordersPending": 342,
  "ordersDelayed": 47,
  "ordersFailed": 81,
  "revenue": 842000.00,
  "currency": "USD",
  "generatedAt": "2026-08-17T08:30:00Z"
}
```

Pending = statuses not in `DELIVERED, CANCELLED, FAILED, PAYMENT_FAILED`.  
Delayed = open shipments past ETA.

### GET `/orders/daily?from=&to=`

### GET `/orders/by-status`

### GET `/warehouses/utilization`

### GET `/products/top?limit=10`

### GET `/alerts`

```json
{
  "alerts": [
    {
      "severity": "WARNING",
      "code": "WAREHOUSE_CAPACITY",
      "message": "Warehouse KGL-01 utilization 82%",
      "at": "2026-08-17T08:00:00Z"
    }
  ]
}
```

Alert rules (evaluated on read + by scheduler every 1 min, stored in `ops_alerts`):

- Warehouse utilization >= 80%
- Delayed shipments >= 10
- Payment failure rate (1h) >= 15%
- Available stock for any tracked SKU <= 10
- DLQ depth > 0 (if metric scrape exposed; otherwise consumer increments counter table)

---

## Gateway routes

| Path | Upstream |
|---|---|
| `/api/v1/auth/**` | auth-service:8081 |
| `/api/v1/users/**` | auth-service:8081 |
| `/api/v1/products/**` | catalog-service:8082 |
| `/api/v1/orders/**` | order-service:8083 |
| `/api/v1/inventory/**` | inventory-service:8084 |
| `/api/v1/warehouses/**` | warehouse-service:8085 |
| `/api/v1/shipments/**` | shipment-service:8086 |
| `/api/v1/payments/**` | payment-service:8087 |
| `/api/v1/notifications/**` | notification-service:8088 |
| `/api/v1/analytics/**` | analytics-service:8089 |

Rate limit: 100 req/min per IP for public auth; 300 req/min per authenticated user otherwise (Redis). 429 `RATE_LIMITED`.
