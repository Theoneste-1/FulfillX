# Database design

Physical layout in Compose: **one PostgreSQL 16 server**, many databases. Logically database-per-service. No cross-database FKs. Cross-service references are UUIDs without constraints.

All tables include no `created_by` in v1 except where noted. Timestamps: `timestamptz`.

---

## fulfillx_auth

```sql
users (
  id              uuid PK,
  email           varchar(320) UNIQUE NOT NULL,
  password_hash   varchar(255) NOT NULL,
  full_name       varchar(200) NOT NULL,
  status          varchar(32) NOT NULL, -- ACTIVE, DISABLED
  created_at      timestamptz NOT NULL,
  updated_at      timestamptz NOT NULL
);

user_roles (
  user_id uuid NOT NULL REFERENCES users(id),
  role    varchar(64) NOT NULL,
  PRIMARY KEY (user_id, role)
);

refresh_tokens (
  id              uuid PK,
  user_id         uuid NOT NULL REFERENCES users(id),
  token_hash      varchar(64) NOT NULL UNIQUE,
  family_id       uuid NOT NULL,
  expires_at      timestamptz NOT NULL,
  revoked_at      timestamptz,
  replaced_by     uuid,
  created_at      timestamptz NOT NULL
);

processed_events (
  event_id     uuid PK,
  event_type   varchar(128) NOT NULL,
  processed_at timestamptz NOT NULL
);

outbox_events (
  id             uuid PK,
  aggregate_type varchar(64) NOT NULL,
  aggregate_id   varchar(64) NOT NULL,
  event_type     varchar(128) NOT NULL,
  payload        jsonb NOT NULL,
  correlation_id varchar(64),
  causation_id   varchar(64),
  schema_version int NOT NULL DEFAULT 1,
  created_at     timestamptz NOT NULL,
  published_at   timestamptz,
  publish_attempts int NOT NULL DEFAULT 0
);
```

Indexes: `refresh_tokens(user_id)`, `refresh_tokens(family_id)`, `outbox_events(published_at, created_at)` WHERE published_at IS NULL.

---

## fulfillx_catalog

```sql
products (
  id          uuid PK,
  sku         varchar(64) UNIQUE NOT NULL,
  name        varchar(200) NOT NULL,
  description text,
  category    varchar(64) NOT NULL,
  price       numeric(19,2) NOT NULL,
  currency    char(3) NOT NULL,
  weight_kg   numeric(10,3),
  active      boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL,
  updated_at  timestamptz NOT NULL
);

-- plus outbox_events, processed_events
```

Indexes: `products(category, active)`, `products(name)`.

---

## fulfillx_order

```sql
orders (
  id                 uuid PK,
  order_number       varchar(32) UNIQUE NOT NULL,
  customer_id        uuid NOT NULL,
  customer_email     varchar(320),
  status             varchar(32) NOT NULL,
  total_amount       numeric(19,2) NOT NULL,
  currency           char(3) NOT NULL,
  shipping_line1     varchar(200) NOT NULL,
  shipping_line2     varchar(200),
  shipping_city      varchar(100) NOT NULL,
  shipping_region    varchar(100),
  shipping_postal    varchar(32),
  shipping_country   char(2) NOT NULL,
  warehouse_id       uuid,
  payment_id         uuid,
  cancellation_reason text,
  created_at         timestamptz NOT NULL,
  updated_at         timestamptz NOT NULL
);

order_items (
  id          uuid PK,
  order_id    uuid NOT NULL REFERENCES orders(id),
  product_id  uuid NOT NULL,
  sku         varchar(64) NOT NULL,
  name        varchar(200) NOT NULL,
  quantity    int NOT NULL CHECK (quantity > 0),
  unit_price  numeric(19,2) NOT NULL
);

order_status_history (
  id          uuid PK,
  order_id    uuid NOT NULL REFERENCES orders(id),
  old_status  varchar(32),
  new_status  varchar(32) NOT NULL,
  reason      varchar(200),
  changed_at  timestamptz NOT NULL
);

idempotency_keys (
  id              uuid PK,
  actor_id        uuid NOT NULL,
  key             varchar(128) NOT NULL,
  request_hash    varchar(64) NOT NULL,
  response_status int NOT NULL,
  response_body   jsonb NOT NULL,
  created_at      timestamptz NOT NULL,
  UNIQUE (actor_id, key)
);

-- plus outbox_events, processed_events
```

Indexes: `orders(customer_id, created_at DESC)`, `orders(status)`, `orders(created_at)`, `order_items(order_id)`, `order_status_history(order_id, changed_at)`.

---

## fulfillx_inventory

```sql
inventory (
  id                 uuid PK,
  warehouse_id       uuid NOT NULL,
  product_id         uuid NOT NULL,
  sku                varchar(64) NOT NULL,
  quantity_on_hand   int NOT NULL CHECK (quantity_on_hand >= 0),
  quantity_reserved  int NOT NULL CHECK (quantity_reserved >= 0),
  version            bigint NOT NULL DEFAULT 0,
  updated_at         timestamptz NOT NULL,
  UNIQUE (warehouse_id, product_id),
  CHECK (quantity_reserved <= quantity_on_hand)
);

inventory_reservations (
  id            uuid PK,
  order_id      uuid NOT NULL UNIQUE,
  warehouse_id  uuid NOT NULL,
  status        varchar(32) NOT NULL, -- ACTIVE, RELEASED, CONSUMED, EXPIRED
  expires_at    timestamptz NOT NULL,
  created_at    timestamptz NOT NULL
);

inventory_reservation_items (
  id               uuid PK,
  reservation_id   uuid NOT NULL REFERENCES inventory_reservations(id),
  product_id       uuid NOT NULL,
  sku              varchar(64) NOT NULL,
  quantity         int NOT NULL
);

inventory_movements (
  id            uuid PK,
  inventory_id  uuid NOT NULL REFERENCES inventory(id),
  type          varchar(32) NOT NULL,
  quantity      int NOT NULL,
  reference_id  uuid,
  created_at    timestamptz NOT NULL
);

warehouse_cache (
  warehouse_id       uuid PK,
  code               varchar(32) NOT NULL,
  country            char(2) NOT NULL,
  city               varchar(100),
  latitude           double precision,
  longitude          double precision,
  capacity           int NOT NULL,
  current_workload   int NOT NULL,
  status             varchar(32) NOT NULL,
  supported_regions  jsonb NOT NULL,
  updated_at         timestamptz NOT NULL
);

-- plus outbox_events, processed_events
```

Indexes: `inventory(product_id)`, `inventory_reservations(status, expires_at)`, `inventory_movements(inventory_id, created_at)`.

---

## fulfillx_warehouse

```sql
warehouses (
  id                uuid PK,
  code              varchar(32) UNIQUE NOT NULL,
  name              varchar(200) NOT NULL,
  country           char(2) NOT NULL,
  city              varchar(100) NOT NULL,
  latitude          double precision NOT NULL,
  longitude         double precision NOT NULL,
  capacity          int NOT NULL,
  current_workload  int NOT NULL DEFAULT 0,
  status            varchar(32) NOT NULL,
  created_at        timestamptz NOT NULL,
  updated_at        timestamptz NOT NULL
);

warehouse_regions (
  warehouse_id uuid NOT NULL REFERENCES warehouses(id),
  country      char(2) NOT NULL,
  PRIMARY KEY (warehouse_id, country)
);

-- plus outbox_events, processed_events
```

---

## fulfillx_payment

```sql
payments (
  id                  uuid PK,
  order_id            uuid NOT NULL,
  amount              numeric(19,2) NOT NULL,
  currency            char(3) NOT NULL,
  status              varchar(32) NOT NULL, -- PENDING, COMPLETED, FAILED, REFUNDED
  provider            varchar(32) NOT NULL,
  provider_reference  varchar(128),
  failure_reason      varchar(128),
  attempt             int NOT NULL DEFAULT 1,
  created_at          timestamptz NOT NULL,
  updated_at          timestamptz NOT NULL
);

UNIQUE (order_id, attempt)

-- plus outbox_events, processed_events
```

---

## fulfillx_shipment

```sql
shipments (
  id                      uuid PK,
  order_id                uuid NOT NULL UNIQUE,
  warehouse_id            uuid NOT NULL,
  carrier                 varchar(32) NOT NULL,
  tracking_number         varchar(64) UNIQUE NOT NULL,
  status                  varchar(32) NOT NULL,
  estimated_delivery      timestamptz NOT NULL,
  actual_delivery         timestamptz,
  delay_event_published   boolean NOT NULL DEFAULT false,
  created_at              timestamptz NOT NULL,
  updated_at              timestamptz NOT NULL
);

shipment_events (
  id            uuid PK,
  shipment_id   uuid NOT NULL REFERENCES shipments(id),
  status        varchar(32) NOT NULL,
  location      varchar(200),
  description   varchar(500),
  occurred_at   timestamptz NOT NULL
);

-- plus outbox_events, processed_events
```

---

## fulfillx_notification

```sql
notifications (
  id           uuid PK,
  order_id     uuid,
  user_id      uuid,
  channel      varchar(16) NOT NULL, -- EMAIL, SMS, PUSH
  template     varchar(64) NOT NULL,
  recipient    varchar(320) NOT NULL,
  subject      varchar(200),
  body         text NOT NULL,
  status       varchar(16) NOT NULL, -- PENDING, SENT, FAILED
  created_at   timestamptz NOT NULL
);

-- plus processed_events (consumes only; outbox if publishing NotificationDispatched)
```

---

## fulfillx_analytics

Star-ish reporting tables, **not** 3NF OLTP. Upserted from events.

```sql
fact_orders (
  order_id         uuid PK,
  order_number     varchar(32),
  customer_id      uuid,
  status           varchar(32) NOT NULL,
  total_amount     numeric(19,2) NOT NULL,
  currency         char(3) NOT NULL,
  warehouse_id     uuid,
  created_at       timestamptz NOT NULL,
  paid_at          timestamptz,
  reserved_at      timestamptz,
  shipped_at       timestamptz,
  delivered_at     timestamptz,
  cancelled_at     timestamptz,
  failed           boolean NOT NULL DEFAULT false
);

daily_order_metrics (
  date                      date PRIMARY KEY,
  orders_created            int NOT NULL DEFAULT 0,
  orders_completed          int NOT NULL DEFAULT 0,
  orders_cancelled          int NOT NULL DEFAULT 0,
  orders_failed             int NOT NULL DEFAULT 0,
  revenue                   numeric(19,2) NOT NULL DEFAULT 0,
  fulfillment_time_seconds  bigint -- running sum for average
);

warehouse_metrics (
  warehouse_id              uuid NOT NULL,
  date                      date NOT NULL,
  orders_processed          int NOT NULL DEFAULT 0,
  failure_rate_numerator    int NOT NULL DEFAULT 0,
  failure_rate_denominator  int NOT NULL DEFAULT 0,
  PRIMARY KEY (warehouse_id, date)
);

product_metrics (
  product_id       uuid NOT NULL,
  sku              varchar(64),
  date             date NOT NULL,
  units_sold       int NOT NULL DEFAULT 0,
  units_reserved   int NOT NULL DEFAULT 0,
  PRIMARY KEY (product_id, date)
);

ops_alerts (
  id          uuid PK,
  severity    varchar(16) NOT NULL,
  code        varchar(64) NOT NULL,
  message     text NOT NULL,
  open        boolean NOT NULL DEFAULT true,
  created_at  timestamptz NOT NULL,
  resolved_at timestamptz
);

-- processed_events
```

---

## Shared outbox / processed_events DDL

Copy the `outbox_events` and `processed_events` definitions into every service that produces or consumes. Flyway `V1__init.sql` per service includes only what that service needs.
