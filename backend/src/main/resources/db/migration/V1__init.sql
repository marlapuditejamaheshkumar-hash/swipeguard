CREATE TABLE card_authorisation (
    id               UUID PRIMARY KEY,
    transaction_id   VARCHAR(64)  NOT NULL,
    card_ref         VARCHAR(64)  NOT NULL,
    masked_pan       VARCHAR(19)  NOT NULL,
    amount           NUMERIC(18,2) NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    mcc              VARCHAR(4)   NOT NULL,
    merchant_country VARCHAR(2)   NOT NULL,
    channel          VARCHAR(4)   NOT NULL,
    tx_timestamp     TIMESTAMP WITH TIME ZONE NOT NULL,
    decision         VARCHAR(8)   NOT NULL,
    score            INT          NOT NULL,
    reasons          TEXT         NOT NULL,
    alert_id         UUID,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    -- idempotency is enforced by the database, not only in memory
    CONSTRAINT uq_card_authorisation_transaction UNIQUE (transaction_id)
);

CREATE TABLE alert (
    id               UUID PRIMARY KEY,
    transaction_id   VARCHAR(64)  NOT NULL,
    card_ref         VARCHAR(64)  NOT NULL,
    masked_pan       VARCHAR(19)  NOT NULL,
    amount           NUMERIC(18,2) NOT NULL,
    currency         VARCHAR(3)   NOT NULL,
    mcc              VARCHAR(4)   NOT NULL,
    merchant_country VARCHAR(2)   NOT NULL,
    decision         VARCHAR(8)   NOT NULL,
    score            INT          NOT NULL,
    reasons          TEXT         NOT NULL,
    status           VARCHAR(16)  NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL
);

-- matches: WHERE status = ? ORDER BY created_at DESC
CREATE INDEX idx_alert_status_created_at ON alert (status, created_at DESC);