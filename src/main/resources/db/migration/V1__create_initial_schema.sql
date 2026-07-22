CREATE TABLE wallets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_wallet_owner_currency
        UNIQUE (owner_id, currency),

    CONSTRAINT chk_wallet_balance_non_negative
        CHECK (balance >= 0)
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    source_wallet_id UUID NOT NULL,
    destination_wallet_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_transfer_source_wallet
        FOREIGN KEY (source_wallet_id)
        REFERENCES wallets (id),

    CONSTRAINT fk_transfer_destination_wallet
        FOREIGN KEY (destination_wallet_id)
        REFERENCES wallets (id),

    CONSTRAINT chk_transfer_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_transfer_different_wallets
        CHECK (source_wallet_id <> destination_wallet_id),

    CONSTRAINT chk_transfer_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL,
    transfer_id UUID,
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ledger_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES wallets (id),

    CONSTRAINT fk_ledger_transfer
        FOREIGN KEY (transfer_id)
        REFERENCES transfers (id),

    CONSTRAINT chk_ledger_entry_type
        CHECK (entry_type IN ('CREDIT', 'DEBIT')),

    CONSTRAINT chk_ledger_amount_positive
        CHECK (amount > 0)
);

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    operation_type VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    resource_id UUID,
    response_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_idempotency_key_operation
        UNIQUE (idempotency_key, operation_type)
);

CREATE INDEX idx_transfers_source_wallet
    ON transfers (source_wallet_id);

CREATE INDEX idx_transfers_destination_wallet
    ON transfers (destination_wallet_id);

CREATE INDEX idx_transfers_created_at
    ON transfers (created_at);

CREATE INDEX idx_ledger_entries_wallet_created
    ON ledger_entries (wallet_id, created_at);

CREATE INDEX idx_ledger_entries_transfer
    ON ledger_entries (transfer_id);

CREATE INDEX idx_idempotency_keys_expires_at
    ON idempotency_keys (expires_at);