# FulfillX

**Distributed order fulfillment and logistics platform.**

A production-style Java 21 / Spring Boot system: database-per-service, Kafka choreography, transactional outbox, race-safe inventory reservation, compensation sagas, JWT security, Prometheus/Grafana, OpenTelemetry traces, and an operations command center.

This repository is meant to be implemented and operated from the docs **without guessing**. Start at [`docs/product-spec.md`](docs/product-spec.md).

---

## What a reviewer should find

| Layer | Where |
|---|---|
| Service boundaries that match the domain | `docs/architecture.md`, `services/` |
| Database-per-service | `infrastructure/postgres/init.sql` |
| Outbox pattern | `libs/outbox`, every producer Flyway |
| Idempotent consumers | `processed_events` + `IdempotentEventProcessor` |
| Inventory concurrency | conditional `UPDATE ... WHERE available >= qty` (ADR-004) |
| Pay-then-stock compensation | `COMPENSATION_REQUIRED` → refund → `CANCELLED` |
| Custom business metrics | `fulfillx.*` Micrometer meters |
| Traces | OpenTelemetry → Jaeger `:16686` |
| Ops intelligence | `frontend/operations-dashboard` + Analytics service |
| Failure matrix | `docs/failure-scenarios.md` |
| ADRs | `docs/architecture-decisions/` |

```text
                              ┌──────────────────┐
                              │ Customer / Admin │
                              └────────┬─────────┘
                                       ▼
                              ┌──────────────────┐
                              │   API Gateway    │
                              └────────┬─────────┘
              ┌────────────────────────┼────────────────────────┐
              ▼                        ▼                        ▼
       Auth Service              Order Service            Catalog Service
                                       │
                                       ▼
                                    Kafka
                ┌──────────────────────┼──────────────────────┐
                ▼                      ▼                      ▼
        Inventory               Payment                Notification
                │
                ▼
           Warehouse → Shipment → Analytics → Operations Dashboard

  Observability: Prometheus → Grafana    OpenTelemetry → Jaeger    JSON logs
```

---

## Quick start

```bash
docker compose up --build
```

| Surface | URL |
|---|---|
| Operations dashboard | http://localhost:3000 |
| API gateway | http://localhost:8080 |
| Grafana (admin/admin) | http://localhost:3001 |
| Prometheus | http://localhost:9090 |
| Jaeger | http://localhost:16686 |
| Swagger (per service) | http://localhost:8081/swagger-ui.html … |

Seeded users are in `docs/product-spec.md` §17. Logistics login for the dashboard:

- `logistics@fulfillx.com` / `Operator123!`

Place an order as `customer@fulfillx.com` / `Customer123!` (see `docs/local-development.md`). Demo mode auto-advances shipments to `DELIVERED`.

Push to GitHub and run this on a VPS with Nginx: [`docs/deploy.md`](docs/deploy.md).

---

## Repository map

```text
fulfillx/
├── deploy/                    VPS scripts (.env example, nginx install, compose up)
├── docs/                      Technical product spec + ADRs
├── libs/                      common, security, outbox
├── services/                  independently deployable Spring Boot apps
├── frontend/operations-dashboard
├── infrastructure/            Compose helpers, Prometheus, Grafana, Docker, Nginx
├── deployment/kubernetes/     Stretch path
├── docker-compose.yml         Local build-from-source
└── docker-compose.prod.yml    Pull GHCR images on a VPS
```

---

## Stack

Java 17+ (21 recommended) · Spring Boot 3.4.12 · Spring Cloud 2024.0.3 · PostgreSQL 16 · Redis · Kafka · Flyway · Testcontainers · Next.js 15 · Prometheus · Grafana · OpenTelemetry

---

## Tests

```bash
mvn verify
```

Requires Docker for Testcontainers. See `docs/testing.md`.

---

## Spec index

1. [Product spec](docs/product-spec.md) — implementer contract
2. [Architecture](docs/architecture.md)
3. [API design](docs/api-design.md)
4. [Event contracts](docs/event-contracts.md)
5. [Database design](docs/database-design.md)
6. [Failure scenarios](docs/failure-scenarios.md)
7. [Security](docs/security.md)
8. [Observability](docs/observability.md)
9. [Local development](docs/local-development.md)
10. [Architecture decisions](docs/architecture-decisions/)
11. [Testing](docs/testing.md)
12. [CI/CD and VPS deploy](docs/deploy.md)
