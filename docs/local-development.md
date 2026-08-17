# Local development

## Prerequisites

- JDK 17 or 21 (Maven compiler target is 17; Compose images use JDK 21)
- Maven 3.9+
- Docker Desktop
- Node.js 22+ (dashboard)
- 16 GB RAM recommended for full Compose

## One-command platform

```bash
docker compose up --build
```

Wait until `api-gateway` is healthy. Then:

- Gateway: http://localhost:8080
- Dashboard: http://localhost:3000
- Grafana: http://localhost:3001 (admin / admin)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686
- Kafka: localhost:9092
- Postgres: localhost:5432 (user `fulfillx` / `fulfillx`)

Login: `admin@fulfillx.com` / `Admin123!`

## IDE mode (services on host)

```bash
docker compose up postgres redis kafka prometheus grafana otel-collector jaeger
```

From repo root:

```bash
mvn -pl services/auth-service -am spring-boot:run
```

`application.yml` defaults already point at `localhost`.

## Tests

```bash
mvn verify
```

Docker required for Testcontainers.

## Demo happy path (curl)

```bash
TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"customer@fulfillx.com","password":"Customer123!"}' | jq -r .accessToken)

# list products, pick an id, then:
curl -s http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: demo-1" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":"<id>","quantity":1}],"shippingAddress":{"line1":"1 Demo St","city":"Kigali","country":"RW"}}'
```

Poll `GET /api/v1/orders/{id}` until `DELIVERED` (demo auto-progress).

To run the same stack on a VPS with GitHub-built images and Nginx, see [`docs/deploy.md`](deploy.md).
