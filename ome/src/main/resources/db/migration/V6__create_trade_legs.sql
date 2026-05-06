CREATE TABLE trade_legs (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_id   UUID          NOT NULL REFERENCES trades(id),
    order_id   UUID          NOT NULL REFERENCES orders(id),
    side       VARCHAR(4)    NOT NULL CHECK (side IN ('BUY','SELL')),
    quantity   DECIMAL(20,8) NOT NULL,
    price      DECIMAL(20,8) NOT NULL,
    fee        DECIMAL(20,8) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_trade_legs_order_id ON trade_legs(order_id);
CREATE INDEX idx_trade_legs_trade_id ON trade_legs(trade_id);