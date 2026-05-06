CREATE TABLE instruments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol           VARCHAR(20)   NOT NULL UNIQUE,
    base_currency    VARCHAR(10)   NOT NULL,
    quote_currency   VARCHAR(10)   NOT NULL,
    tick_size        DECIMAL(20,8) NOT NULL,
    lot_size         DECIMAL(20,8) NOT NULL,
    min_order_value  DECIMAL(20,8) NOT NULL DEFAULT 1,
    trading_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_instruments_symbol ON instruments(symbol);