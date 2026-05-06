CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users(id),
    instrument_id   UUID          NOT NULL REFERENCES instruments(id),
    client_order_id VARCHAR(64)   UNIQUE,
    side            VARCHAR(4)    NOT NULL CHECK (side IN ('BUY','SELL')),
    type            VARCHAR(10)   NOT NULL CHECK (type IN ('LIMIT','MARKET','STOP')),
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','OPEN','PARTIALLY_FILLED',
                                      'FILLED','CANCELLED','REJECTED')),
    quantity        DECIMAL(20,8) NOT NULL CHECK (quantity > 0),
    filled_qty      DECIMAL(20,8) NOT NULL DEFAULT 0,
    remaining_qty   DECIMAL(20,8) GENERATED ALWAYS AS (quantity - filled_qty) STORED,
    price           DECIMAL(20,8),
    avg_fill_price  DECIMAL(20,8),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_status
    ON orders(user_id, status)
    WHERE status IN ('OPEN','PARTIALLY_FILLED','PENDING');

CREATE INDEX idx_orders_instrument_status
    ON orders(instrument_id, status, created_at)
    WHERE status IN ('OPEN','PARTIALLY_FILLED');

CREATE INDEX idx_orders_user_created
    ON orders(user_id, created_at DESC);