# FulfillX — Technical Product Specification

**Document status:** Implementation-ready  
**Version:** 1.0.0  
**Last updated:** 2026-08-17  
**Audience:** Backend engineers implementing or extending the platform without clarifying questions.

This document is the source of truth. If code and this spec diverge, treat the spec as the intended contract and file an ADR before changing it.

---

## 0. How to read this document

Implement in this order:

1. Shared libraries (`libs/`)
2. Infrastructure (`docker-compose.yml`)
3. Auth → Catalog → Warehouse → Inventory
4. Order → Payment → Shipment
5. Notification → Analytics
6. API Gateway
7. Operations dashboard
8. Observability (Prometheus, Grafana, OpenTelemetry)

Do not invent parallel APIs, extra statuses, or extra Kafka topics. If a gap is real, add an ADR under `docs/architecture-decisions/`.

**Clock:** UTC everywhere (`Instant`, `timestamptz`).  
**IDs:** UUID v4 unless a human-readable number is specified (`orderNumber`, `trackingNumber`, `sku`).  
**Money:** `NUMERIC(19,2)` / `BigDecimal` with scale 2. Never `double`/`float`.  
**Currency:** ISO-4217, default `USD`.  
**Language:** English for logs, events, and API error messages.

---

## 1. Problem statement

FulfillX is a production-style **distributed order fulfillment and logistics platform**. It owns the lifecycle of an order from placement through delivery, and it exposes operational intelligence so a logistics team can answer:

| Question | Owner |
|---|---|
| Where is this order? | Order + Shipment |
| Which warehouse is fulfilling it? | Inventory + Warehouse |
| Is inventory available? | Inventory |
| Was payment successful? | Payment |
| Why did fulfillment fail? | Order status history + compensation |
| How many orders are delayed? | Analytics |
| Which warehouse is overloaded? | Warehouse + Analytics |
| Which products are close to stockout? | Inventory + Analytics |
| How long does fulfillment take? | Analytics + custom metrics |
| Are services healthy? | Prometheus / Grafana |
| Are orders stuck in a state? | Analytics + Order status age |

Microservices exist because these responsibilities **evolve independently**, fail independently, and scale independently — not because “microservices look impressive.”

---

## 2. Goals and non-goals

### Goals (v1)

- End-to-end order lifecycle with explicit state machine
- Database-per-service
- Kafka domain events with a versioned envelope
- Transactional outbox on every producing service
- Idempotent consumers (`processed_events`)
- Inventory reservation that cannot oversell under concurrency
- Compensation when payment succeeds and inventory fails
- JWT auth with role-based access
- Operations dashboard fed by the Analytics service
- Prometheus + Grafana + OpenTelemetry traces + JSON logs
- Testcontainers integration tests and one golden-path e2e test

### Non-goals (v1)

- Real SMS/email providers (channels are abstracted; email is persisted + logged)
- Real card-network payment (Payment Service is a sandbox simulator)
- Multi-tenancy / marketplace of sellers
- Real-time GPS tracking from carriers
- Kubernetes (Compose is v1; manifests live under `deployment/kubernetes/` as a stretch path)
- CQRS with separate read DBs inside transactional services
- Event sourcing as the system of record (events are integration events, not the write model)

---

## 3. Primary stack

| Layer | Choice | Why |
|---|---|---|
| Language | Java 17+ (21 recommended; Docker images use Temurin 21) | Records, current LTS family |
| Framework | Spring Boot 3.4.12 | Production default for this stack |
| Cloud | Spring Cloud 2024.0.3 | Gateway, OpenFeign, circuit breaker |
| DB | PostgreSQL 16 | Relational integrity, `FOR UPDATE`, JSON |
| Cache / rate limit | Redis 7 | Gateway rate limit, catalog cache, token denylist |
| Messaging | Apache Kafka 3.9 | Service decoupling, analytics fan-out |
| Migrations | Flyway | Repeatable schema ownership |
| Auth | Spring Security + JWT (HS256 local, RS256 documented for prod) | Stateless APIs |
| Observability | Actuator + Prometheus + Grafana + OTel + Jaeger | Metrics, traces, dashboards |
| Tests | JUnit 5 + Testcontainers | Real Postgres + Kafka |
| UI | Next.js 15 + React 19 + Tailwind | Operations Command Center |
| Runtime | Docker Compose | One-command local platform |

---

## 4. Service map and why each exists

| Service | Port | Database | Why it is a separate service |
|---|---|---|---|
| api-gateway | 8080 | — | Authn at the edge, routing, rate limit, CORS, correlation ID |
| auth-service | 8081 | fulfillx_auth | Identity, tokens, roles. Must not own orders or stock |
| catalog-service | 8082 | fulfillx_catalog | Product master data evolves with merchandising, not fulfillment |
| order-service | 8083 | fulfillx_order | Order aggregate + state machine is the core domain |
| inventory-service | 8084 | fulfillx_inventory | Stock reservation is a concurrency domain of its own |
| warehouse-service | 8085 | fulfillx_warehouse | Physical network, capacity, regions |
| payment-service | 8087 | fulfillx_payment | PCI-adjacent; sandbox now, replaceable later |
| shipment-service | 8086 | fulfillx_shipment | Carrier/tracking lifecycle |
| notification-service | 8088 | fulfillx_notification | Fan-out of customer/ops messages |
| analytics-service | 8089 | fulfillx_analytics | Reporting models must not live in OLTP schemas |

**Rule:** no service queries another service’s database. Communication is REST (request/response) or Kafka (facts that already happened).

---

## 5. Runtime topology

```text
 Client (Customer app / Operations Dashboard)
                    │
                    ▼
              API Gateway :8080
                    │
     ┌──────────────┼──────────────┐
     ▼              ▼              ▼
   Auth          Order          Catalog
                    │
                    │  OrderCreated (outbox → Kafka)
                    ▼
                  Kafka
         ┌──────────┼──────────┐
         ▼          ▼          ▼
      Payment   Inventory  Notification
                    │
                    ▼
                Warehouse (REST for selection + events for master data)
                    │
                    ▼
                Shipment
                    │
                    ▼
                Analytics ──► Operations Dashboard :3000

 Observability plane (orthogonal):
   every service ──► /actuator/prometheus ──► Prometheus ──► Grafana
   every service ──► OTLP ──► OpenTelemetry Collector ──► Jaeger
   every service ──► JSON stdout (traceId, orderId, service)
```

---

## 6. Identity, roles, and authorization

### Roles

| Role | Meaning |
|---|---|
| CUSTOMER | Places and views own orders |
| WAREHOUSE_OPERATOR | Warehouse ops, pick/pack, inventory adjustments |
| LOGISTICS_OPERATOR | Shipments, delays, carrier assignment |
| SUPPORT | Read orders/shipments across customers; cannot refund without ADMIN |
| ADMIN | Users, catalog writes, system operations, compensation replay |

### Token contract

- **Access token:** JWT, 15 minutes, header `Authorization: Bearer <token>`
- **Refresh token:** opaque, 7 days, stored **hashed** (SHA-256) in `refresh_tokens`
- **Issuer:** `fulfillx-auth`
- **Audience:** `fulfillx-api`
- **Claims:** `sub` (user UUID), `email`, `roles` (array), `typ=access`
- **Clock skew:** 30 seconds

### Hard security rules

1. Never trust `customerId` from a request body to decide ownership. Use `SecurityContext` (`sub`).
2. CUSTOMER may only read/write **their** orders.
3. Actuator besides `/health` and `/info` is not exposed through the gateway.
4. Passwords: BCrypt strength 12.
5. Refresh rotation: old refresh token is revoked on use. Reuse of a revoked token revokes the whole family.
6. Gateway validates JWT. Services re-validate (defense in depth) using the same secret/keys.

### Public routes (no JWT)

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /actuator/health`
- `GET /api/v1/products` (public catalog browse)
- `GET /api/v1/products/{id}`

---

## 7. Cross-cutting HTTP contract

### Versioning

All public APIs: `/api/v1/...`

### Required headers

| Header | Set by | Required |
|---|---|---|
| `Authorization` | Client | Yes except public routes |
| `X-Correlation-Id` | Client or Gateway | Gateway generates UUID if missing and forwards |
| `Idempotency-Key` | Client | Required on `POST /api/v1/orders` and `POST /api/v1/payments/{id}/retry` |
| `Content-Type` | Client | `application/json` |

Gateway also sets `X-Request-Id` (unique per hop) and forwards `traceparent` when present.

### Error body (every 4xx/5xx)

```json
{
  "timestamp": "2026-08-17T08:30:00Z",
  "status": 409,
  "error": "Conflict",
  "code": "INSUFFICIENT_INVENTORY",
  "message": "SKU FX-MOUSE-01 has 2 available in selected warehouse, requested 5",
  "path": "/api/v1/inventory/reservations",
  "correlationId": "c1b0e0a0-....",
  "traceId": "4bf92f3577b34da6"
}
```

Never return stack traces. Log them with `traceId`.

### Pagination

Query: `page` (0-based), `size` (default 20, max 100), `sort` (`field,asc|desc`).

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Idempotency

For `POST /api/v1/orders`:

- Key scoped to authenticated user.
- Same key + same payload hash → return original response (200/201).
- Same key + different payload → `409 IDEMPOTENCY_KEY_REUSED`.
- Keys expire after 24 hours.

---

## 8. Domain — Order lifecycle

### Aggregates

**Order** is the consistency boundary. Items cannot exist without an order. Status changes are recorded in `order_status_history` with `reason` and `changed_at`.

### Statuses (closed set)

| Status | Meaning | Terminal? |
|---|---|---|
| CREATED | Persisted, not yet submitted to payment | No |
| PAYMENT_PENDING | Waiting for Payment Service | No |
| PAYMENT_FAILED | Payment declined / simulator fail | Yes* |
| PAID | Funds captured (sandbox) | No |
| INVENTORY_PENDING | Waiting for reservation | No |
| INVENTORY_FAILED | Could not reserve | No (compensation) |
| RESERVED | Stock held | No |
| PROCESSING | Warehouse picking | No |
| SHIPPED | Handed to carrier | No |
| DELIVERED | Customer received | Yes |
| COMPENSATION_REQUIRED | Paid but cannot fulfill | No |
| REFUND_PENDING | Refund requested | No |
| CANCELLED | Terminal cancelled (may include refunded) | Yes |
| FAILED | Terminal unrecoverable | Yes |

\* `PAYMENT_FAILED` is terminal for that attempt; customer may place a **new** order. No silent retry of the same order without an explicit retry API.

### Allowed transitions

```
CREATED → PAYMENT_PENDING | CANCELLED
PAYMENT_PENDING → PAID | PAYMENT_FAILED | CANCELLED
PAID → INVENTORY_PENDING | COMPENSATION_REQUIRED | CANCELLED
INVENTORY_PENDING → RESERVED | INVENTORY_FAILED
INVENTORY_FAILED → COMPENSATION_REQUIRED
RESERVED → PROCESSING | CANCELLED (releases reservation)
PROCESSING → SHIPPED | CANCELLED (releases reservation if not yet picked)
SHIPPED → DELIVERED
COMPENSATION_REQUIRED → REFUND_PENDING
REFUND_PENDING → CANCELLED
```

Any other transition throws `ILLEGAL_ORDER_TRANSITION` (409).

### Order number

Format: `ORD-YYYYMMDD-<6 alphanumeric>`  
Example: `ORD-20260817-K4H91Q`  
Unique globally in `order_db`.

### Price snapshot

At order creation, copy `sku`, `name`, `unitPrice`, `currency` from Catalog. Later catalog price changes **must not** mutate historical orders.

---

## 9. Domain — Inventory and warehouses

### Availability

```
available = quantity_on_hand - quantity_reserved
```

`available` is never stored. Always computed.

### Reservation algorithm (must be race-safe)

Two concurrent orders for SKU with `available = 5`, quantities 4 and 3: **exactly one** succeeds; the other gets `INSUFFICIENT_INVENTORY`.

**Chosen implementation (ADR-004):** atomic conditional update:

```sql
UPDATE inventory
SET quantity_reserved = quantity_reserved + :qty,
    version = version + 1,
    updated_at = now()
WHERE warehouse_id = :warehouseId
  AND product_id = :productId
  AND (quantity_on_hand - quantity_reserved) >= :qty;
```

If `rowCount == 0` → fail that warehouse and try next, or fail the reservation.

Additionally keep JPA `@Version` for non-reservation updates.

### Warehouse selection (Inventory Service)

Ordered rules (first failure eliminates the candidate):

1. Product has `available >= requested` in that warehouse
2. Warehouse `status = OPERATIONAL`
3. Warehouse supports the customer’s destination country (`supported_regions`)
4. `current_workload < capacity`
5. Remaining candidates scored by: distance (haversine) * 0.6 + (workload/capacity) * 0.3 + (1 / available) * 0.1
6. Lowest score wins. Tie-breaker: warehouse `code` ascending.

Inventory Service calls Warehouse Service over REST (cached in Redis 30s). If Warehouse Service is down, **fallback:** pick the warehouse with highest available stock among those that have stock (circuit breaker fallback). Log `WAREHOUSE_SELECTION_DEGRADED`.

### Reservation expiry

- TTL: 15 minutes from `created_at` while order is not yet `PROCESSING`
- Scheduler every 30 seconds releases expired `ACTIVE` reservations
- Publishes `InventoryReleased`
- Order Service moves `RESERVED` / `INVENTORY_PENDING` orders that lost reservation to `INVENTORY_FAILED` then compensation if already `PAID`

### Inventory movement types

`RECEIVED`, `RESERVED`, `RELEASED`, `SHIPPED`, `ADJUSTED`, `RETURNED`

Every quantity change writes `inventory_movements`. This is the audit trail.

---

## 10. Payment (sandbox)

Payment Service is **not** Stripe. It is a replaceable sandbox.

Rules:

- Default: succeed after 200–800ms simulated latency
- Fail if request header `X-Simulate-Payment-Failure: true` (propagated in OrderCreated payload `simulation.paymentFail`)
- Fail if order `totalAmount` cents `% 100 == 13` (deterministic demo fail: $x.13)
- Idempotent on `orderId` — one Payment row per order
- Publishes `PaymentCompleted` or `PaymentFailed`
- Refunds: `POST` internal command via `RefundRequested` event → `PaymentRefunded`

Never store PAN. Store `last4` only if a fake method is used (`****1111`).

---

## 11. Shipment lifecycle

```
CREATED → ASSIGNED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
CREATED → FAILED
any non-terminal → DELAYED → (resume previous or IN_TRANSIT)
```

`trackingNumber`: `FX-<CARRIER>-<12 alphanumeric>`  
Carriers v1: `FXL` (FulfillX Logistics), `DHL`, `FEDEX`, `UPS`

Demo mode (`fulfillx.demo.auto-progress-shipments=true`): a scheduler advances shipments on an interval so the dashboard is alive.

Delay rule for analytics: `estimated_delivery < now()` AND status not `DELIVERED` → `ShipmentDelayed` event once per shipment (idempotent flag `delay_event_published`).

---

## 12. Kafka

### Topics (closed set)

| Topic | Producers | Consumers |
|---|---|---|
| `orders.events` | order-service | payment, inventory, notification, analytics, shipment |
| `payments.events` | payment-service | order, notification, analytics |
| `inventory.events` | inventory-service | order, notification, analytics, shipment |
| `warehouses.events` | warehouse-service | inventory (cache invalidation), analytics |
| `shipments.events` | shipment-service | order, notification, analytics |
| `notifications.events` | notification-service | analytics (optional) |
| `*.events.dlq` | each consumer error handler | ops replay |

Partitions: 3 in Compose. Replication: 1 locally.

### Envelope (every message)

```json
{
  "eventId": "8f14e45f-ea31-4d0c-9a5b-1c2d3e4f5a6b",
  "eventType": "OrderCreated",
  "occurredAt": "2026-08-17T08:30:00Z",
  "aggregateType": "Order",
  "aggregateId": "2c6ee24b-....",
  "schemaVersion": 1,
  "correlationId": "c1b0e0a0-....",
  "causationId": null,
  "payload": {}
}
```

- `eventId` is the idempotency key for consumers
- `correlationId` is the original customer request
- `causationId` is the `eventId` that caused this event (null for the first)

### Consumer rules

1. If `eventId` exists in `processed_events` → ack and skip
2. Process in a DB transaction: business write + insert `processed_events`
3. Retry transient errors (3 attempts, exponential backoff 1s/5s/30s)
4. After retries: publish original record to `{topic}.dlq` with header `x-exception-message`, then ack
5. Never throw forever and block the partition

### Outbox (every producer)

Same Postgres transaction as the business write:

1. Insert/update aggregate
2. Insert `outbox_events` row (`published_at IS NULL`)
3. Commit
4. `OutboxPublisher` (every 500ms) sends to Kafka, then sets `published_at`

Polling uses `SELECT ... FOR UPDATE SKIP LOCKED` so multiple instances are safe.

---

## 13. Failure / compensation matrix

| Scenario | System behavior |
|---|---|
| DB commit succeeds, Kafka down | Outbox retains row; publisher retries; no data loss |
| Payment success, inventory fail | Order → `INVENTORY_FAILED` → `COMPENSATION_REQUIRED` → `RefundRequested` → `CANCELLED` |
| Inventory reserved, customer cancels | `InventoryReleased`; order `CANCELLED` |
| Reservation TTL expires | Release stock; if PAID, compensation/refund path |
| Duplicate Kafka event | Ignored via `processed_events` |
| Shipment service down | Order stays `PROCESSING`; not marked SHIPPED |
| Payment service down | Order stays `PAYMENT_PENDING`; outbox already has OrderCreated; payment consumes later |
| Warehouse service down | Degraded warehouse selection (stock-only) |
| Poison message | DLQ; Grafana alert; manual replay API for ADMIN |

---

## 14. Observability split

**Business analytics** (Analytics DB + Operations dashboard): orders/day, revenue, fulfillment time, warehouse utilization, cancellation rate, stockout risk.

**Platform observability** (Prometheus/Grafana): RPS, error rate, P95/P99 latency, JVM, Kafka lag, DB pool, DLQ depth.

Custom meters (Micrometer):

- `fulfillx.orders.created`
- `fulfillx.orders.completed`
- `fulfillx.orders.failed`
- `fulfillx.inventory.reservations`
- `fulfillx.inventory.reservation.failures`
- `fulfillx.payments.failures`
- `fulfillx.shipments.delays`
- `fulfillx.order.fulfillment.duration` (timer, seconds)

---

## 15. SLOs (v1 local/demo targets)

| SLO | Target |
|---|---|
| Order create API P95 | < 400ms excluding payment wait (async) |
| Availability of Gateway | 99.5% monthly (aspirational) |
| Outbox publish delay P99 | < 5s |
| Inventory reservation correctness | 0 oversells |
| Trace coverage | 100% of `/api/v1/**` requests |

---

## 16. Environments

| Name | How |
|---|---|
| local-ide | Compose for Postgres/Kafka/Redis/observability; services from Maven |
| local-compose | Everything in Docker |
| test | Testcontainers |
| prod | Out of scope; RS256, managed Kafka, secrets manager, K8s |

Default credentials are **local only**. See `docs/local-development.md`.

---

## 17. Seed users (local)

| Email | Password | Role |
|---|---|---|
| admin@fulfillx.com | Admin123! | ADMIN |
| warehouse@fulfillx.com | Operator123! | WAREHOUSE_OPERATOR |
| logistics@fulfillx.com | Operator123! | LOGISTICS_OPERATOR |
| support@fulfillx.com | Support123! | SUPPORT |
| customer@fulfillx.com | Customer123! | CUSTOMER |

---

## 18. Definition of done for an implementing engineer

A story is not done until:

1. Flyway migration is in the owning service
2. API matches this spec (status codes + error codes)
3. Events use the envelope and listed `eventType`
4. Writes that emit events use the outbox
5. Consumers are idempotent
6. Tests: unit for the rule + Testcontainers for the persistence/messaging path
7. Metric + structured log on failure paths
8. README / OpenAPI updated if a public API changed

If you would have to ask “what did they mean?”, the spec failed — extend **this file** and an ADR, then code.
