CREATE TABLE trades (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrument_id UUID          NOT NULL REFERENCES instruments(id),
    price         DECIMAL(20,8) NOT NULL,
    quantity      DECIMAL(20,8) NOT NULL,
    executed_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_trades_instrument_time
    ON trades(instrument_id, executed_at DESC);