CREATE TABLE inventory (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    quantity_on_hand INT NOT NULL CHECK (quantity_on_hand >= 0),
    quantity_reserved INT NOT NULL CHECK (quantity_reserved >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    UNIQUE (warehouse_id, product_id),
    CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    warehouse_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE inventory_reservation_items (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES inventory_reservations (id),
    product_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    quantity INT NOT NULL
);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    inventory_id UUID NOT NULL REFERENCES inventory (id),
    type VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    reference_id UUID,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE warehouse_cache (
    warehouse_id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    country CHAR(2) NOT NULL,
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    capacity INT NOT NULL,
    current_workload INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    supported_regions TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
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

CREATE INDEX idx_inventory_product ON inventory (product_id);
CREATE INDEX idx_reservations_expiry ON inventory_reservations (status, expires_at);
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
