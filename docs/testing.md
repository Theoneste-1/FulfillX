# Testing strategy

## Unit

Business rules with no Spring context:

- `OrderStateMachineTest` — every legal and illegal transition
- `InventoryAvailabilityTest` — `on_hand - reserved`
- `WarehouseSelectionTest` — scoring order
- Password / role constants

## Integration (Testcontainers)

- PostgreSQL for Flyway + repository
- Kafka for consumer idempotency (duplicate `eventId` inserts one payment)

## Contract

Feign Catalog DTO must match Catalog `ProductResponse` field names (camelCase). Event payload keys must match `docs/event-contracts.md`.

## Golden-path e2e (manual or future module)

1. Register / login customer
2. GET products
3. POST order with Idempotency-Key
4. Poll until `DELIVERED` (demo auto-progress) or `CANCELLED` if `$x.13` payment fail
5. Analytics overview increments

Run against Compose, not mocks.
