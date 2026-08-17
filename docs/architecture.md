# Architecture

## Style

FulfillX is a **modular monolith of independently deployable services** coordinated by:

- Synchronous REST for *questions* (is this SKU valid? which warehouse is operational?)
- Asynchronous Kafka for *facts* (order created, payment completed, inventory reserved)

Saga orchestration is **choreography**, not a central orchestrator. Order Service is the state authority for the order aggregate; other services do not update `orders`.

## Why not a monolith?

A monolith would still work at this scale. Services are split because:

| Boundary | Independent reason |
|---|---|
| Auth | Different threat model, token issuance |
| Catalog | Read-heavy, cacheable, merchandising lifecycle |
| Order | Write model + state machine |
| Payment | Replaceable provider, failure isolation |
| Inventory | Contention and locking strategy |
| Warehouse | Physical network master data |
| Shipment | Carrier integrations |
| Notification | Unreliable third parties |
| Analytics | Different data lifetime and query patterns |

If two modules always deploy together and share a table, they should have been one service. That is why Inventory does **not** own warehouse geo/capacity, and why Analytics does **not** query `order_db`.

## Consistency model

- **Inside a service:** ACID via PostgreSQL.
- **Across services:** eventual consistency via events + outbox.
- **User-visible order status:** strongly consistent in Order Service. Clients poll `GET /api/v1/orders/{id}` or the dashboard.

There is no two-phase commit across services.

## Synchronous calls (allowed)

| Caller | Callee | Why sync |
|---|---|---|
| order-service | catalog-service | Need current price/SKU to snapshot before commit |
| inventory-service | warehouse-service | Need operational/capacity/region data for selection |
| api-gateway | all | HTTP routing |

All other collaboration is Kafka.

Timeouts: 1s connect, 2s read. Retry: only GET, max 2 retries. Circuit breaker: open after 50% failures in a 10-call window.

## Package layout (every Spring MVC service)

```text
com.fulfillx.<service>
  <Service>Application.java
  api/              # controllers, request/response records
  application/      # use cases, state machines
  domain/           # entities, enums, domain errors
  infrastructure/
    persistence/
    messaging/
    client/         # Feign
  config/
```

## Ports

See product spec. Compose service names match folder names (`order-service`, etc.).

## Configuration

- `application.yml` inside each service: defaults for local-ide
- Environment variables override in Compose (`SPRING_DATASOURCE_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`, `OTEL_EXPORTER_OTLP_ENDPOINT`)
- Never commit real secrets. Local JWT secret is a documented demo value.

## Demo mode

`FULFILLX_DEMO=true` (default in Compose):

- Payment simulator
- Auto-progress shipments
- Seed catalog, warehouses, inventory, and a week of analytics facts
