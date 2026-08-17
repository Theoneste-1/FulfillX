CREATE TABLE fact_orders (
    order_id UUID PRIMARY KEY,
    order_number VARCHAR(32),
    customer_id UUID,
    status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    warehouse_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    reserved_at TIMESTAMPTZ,
    shipped_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    failed BOOLEAN NOT NULL DEFAULT FALSE,
    delayed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_fact_orders_status ON fact_orders (status);
CREATE INDEX idx_fact_orders_created ON fact_orders (created_at);
CREATE INDEX idx_fact_orders_warehouse ON fact_orders (warehouse_id);

CREATE TABLE daily_order_metrics (
    date DATE PRIMARY KEY,
    orders_created INT NOT NULL DEFAULT 0,
    orders_completed INT NOT NULL DEFAULT 0,
    orders_cancelled INT NOT NULL DEFAULT 0,
    orders_failed INT NOT NULL DEFAULT 0,
    revenue NUMERIC(19, 2) NOT NULL DEFAULT 0,
    fulfillment_time_seconds BIGINT
);

CREATE TABLE warehouse_metrics (
    warehouse_id UUID NOT NULL,
    date DATE NOT NULL,
    orders_processed INT NOT NULL DEFAULT 0,
    failure_rate_numerator INT NOT NULL DEFAULT 0,
    failure_rate_denominator INT NOT NULL DEFAULT 0,
    PRIMARY KEY (warehouse_id, date)
);

CREATE TABLE product_metrics (
    product_id UUID NOT NULL,
    sku VARCHAR(64),
    date DATE NOT NULL,
    units_sold INT NOT NULL DEFAULT 0,
    units_reserved INT NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, date)
);

CREATE TABLE ops_alerts (
    id UUID PRIMARY KEY,
    severity VARCHAR(16) NOT NULL,
    code VARCHAR(64) NOT NULL,
    message TEXT NOT NULL,
    open BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_ops_alerts_open_code ON ops_alerts (open, code);

CREATE TABLE warehouse_snapshot (
    warehouse_id UUID PRIMARY KEY,
    code VARCHAR(32),
    name VARCHAR(200),
    capacity INT,
    current_workload INT,
    status VARCHAR(32),
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE stock_snapshot (
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(64),
    available INT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (warehouse_id, product_id)
);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_processed_events_type_at ON processed_events (event_type, processed_at);
