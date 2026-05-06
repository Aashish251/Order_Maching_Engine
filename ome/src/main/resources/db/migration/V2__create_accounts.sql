CREATE TABLE accounts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID          NOT NULL REFERENCES users(id),
    currency          VARCHAR(10)   NOT NULL,
    available_balance DECIMAL(20,8) NOT NULL DEFAULT 0
                      CHECK (available_balance >= 0),
    reserved_balance  DECIMAL(20,8) NOT NULL DEFAULT 0
                      CHECK (reserved_balance >= 0),
    version           BIGINT        NOT NULL DEFAULT 0,
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_account_user_currency UNIQUE (user_id, currency)
);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);