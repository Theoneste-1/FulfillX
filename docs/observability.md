# Observability

## Three pillars

| Pillar | Tool | Use |
|---|---|---|
| Metrics | Micrometer → Prometheus → Grafana | SLO burn, Kafka lag, business counters |
| Traces | OpenTelemetry → Collector → Jaeger | Hop-by-hop latency for one order |
| Logs | JSON stdout | Join with `traceId` and `orderId` |

Business analytics is **not** this file. See Analytics service and Operations dashboard.

## Actuator

Exposed internally:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`
- `/actuator/metrics`

Gateway does not route `/actuator/**` from the internet.

## Tracing

- `management.tracing.sampling.probability: 1.0` in local/demo
- W3C `traceparent`
- Kafka: Micrometer tracing interceptors so a trace continues Order → Kafka → Payment

Jaeger UI: `http://localhost:16686`

## Grafana dashboards (provisioned)

1. **Platform Health** — RPS, 5xx, P95/P99, CPU, heap, DB connections
2. **Microservices** — per-service rate/error/duration
3. **Kafka** — consumer lag, DLQ counter
4. **Business** — custom `fulfillx.*` meters

Provision JSON lives in `infrastructure/grafana/dashboards/`.

## Log shape

```json
{
  "timestamp": "2026-08-17T08:30:00.123Z",
  "level": "ERROR",
  "service": "inventory-service",
  "traceId": "4bf92f3577b34da6",
  "spanId": "00f067aa0ba902b7",
  "correlationId": "c1b0e0a0-....",
  "orderId": "ORD-20260817-K4H91Q",
  "message": "Inventory reservation failed",
  "errorCode": "INSUFFICIENT_INVENTORY"
}
```

Logback encoder: `net.logstash.logback` **or** Spring `structured` JSON (Boot 3.4). Use Boot 3.4 native JSON logging:

```yaml
logging:
  structured:
    format:
      console: ecs
```

If ECS is too heavy, use `json` format. MDC keys: `correlationId`, `orderId`, `service`.

## Custom meters

Registered in `com.fulfillx.common.observability.FulfillxMetrics`.

Naming: Micrometer dots → Prometheus underscores (`fulfillx_orders_created_total`).
