CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(32) UNIQUE NOT NULL,
    customer_id UUID NOT NULL,
    customer_email VARCHAR(320),
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    shipping_line1 VARCHAR(200) NOT NULL,
    shipping_line2 VARCHAR(200),
    shipping_city VARCHAR(100) NOT NULL,
    shipping_region VARCHAR(100),
    shipping_postal VARCHAR(32),
    shipping_country CHAR(2) NOT NULL,
    warehouse_id UUID,
    payment_id UUID,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_orders_customer_created ON orders (customer_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id),
    product_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

CREATE TABLE order_status_history (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id),
    old_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    reason VARCHAR(200),
    changed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_order_status_history_order ON order_status_history (order_id, changed_at);

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INT NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (actor_id, key)
);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    correlation_id VARCHAR(64),
    causation_id VARCHAR(64),
    schema_version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    publish_attempts INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
