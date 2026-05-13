-- V10__partition_orders.sql
-- NOTE: Run this only when ready to migrate — requires downtime or pg_partman
-- Shown here for design documentation purposes

-- Step 1: Rename existing table
ALTER TABLE orders RENAME TO orders_old;

-- Step 2: Create partitioned table
CREATE TABLE orders (
    id              UUID          NOT NULL,
    user_id         UUID          NOT NULL,
    instrument_id   UUID          NOT NULL,
    client_order_id VARCHAR(64),
    side            VARCHAR(4)    NOT NULL,
    type            VARCHAR(10)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    quantity        DECIMAL(20,8) NOT NULL,
    filled_qty      DECIMAL(20,8) NOT NULL DEFAULT 0,
    remaining_qty   DECIMAL(20,8) GENERATED ALWAYS AS (quantity - filled_qty) STORED,
    price           DECIMAL(20,8),
    avg_fill_price  DECIMAL(20,8),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);    -- partition by month

-- Step 3: Create monthly partitions
CREATE TABLE orders_2026_01 PARTITION OF orders
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE orders_2026_02 PARTITION OF orders
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

-- Add more partitions as needed — automate with pg_partman in production

-- Step 4: Migrate data
INSERT INTO orders SELECT * FROM orders_old;

-- Step 5: Recreate indexes on partitioned table
CREATE INDEX idx_orders_user_status ON orders(user_id, status)
    WHERE status IN ('OPEN','PARTIALLY_FILLED','PENDING');

CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);