# Security

## Trust boundaries

- Internet → Gateway
- Gateway → services (Docker network)
- Services → their own Postgres
- Services → Kafka
- Services → Redis (gateway + auth denylist + inventory warehouse cache)

Postgres and Kafka ports may be published on localhost for developer convenience. Production: private subnets only.

## JWT

Local: HS256, secret `FULFILLX_JWT_SECRET` (Compose default is a **demo** 256-bit string). Production ADR-006: RS256, auth-service issues, gateway and services validate with JWKS.

Denylist: logout puts access `jti` in Redis `auth:denylist:{jti}` with TTL = remaining token life. Resource servers should check denylist; v1 gateway checks Redis; services trust gateway + re-parse signature. Full denylist check in each service is optional if Redis is shared; implement in `fulfillx-security` filter when `fulfillx.security.denylist-enabled=true`.

## Password policy

Min 8, at least one uppercase, one lowercase, one digit. BCrypt 12.

## Authorization matrix (summary)

| Endpoint family | CUSTOMER | WAREHOUSE_OP | LOGISTICS_OP | SUPPORT | ADMIN |
|---|---|---|---|---|---|
| Auth me/logout | Y | Y | Y | Y | Y |
| Catalog write | | | | | Y |
| Catalog read | Y | Y | Y | Y | Y |
| Create order | Y (self) | | | | Y |
| Get own order | Y | | | | Y |
| Get any order | | | Y | Y | Y |
| Cancel own | Y* | | | Y | Y |
| Inventory write | | Y | | | Y |
| Warehouse write | | | | | Y |
| Shipment status | | | Y | | Y |
| Analytics | | Y | Y | Y | Y |

\* before SHIPPED

## Input validation

Bean Validation on all controllers. Quantity > 0. Country ISO-3166 alpha-2. Currency ISO-4217.

## Injection / XSS

JPA parameterized queries only. JSON logs. Dashboard React-escapes text.

## Secrets

No secrets in git. `.env.example` lists names only.
