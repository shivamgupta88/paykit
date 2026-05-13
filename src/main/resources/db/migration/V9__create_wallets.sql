-- Tenant wallets: one per tenant, tracks available balance
CREATE TABLE tenant_wallets (
    id              UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id       UUID        NOT NULL UNIQUE REFERENCES tenants(id),
    balance         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_earned    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_withdrawn NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_by      UUID,
    version         BIGINT      NOT NULL DEFAULT 0
);

-- Payout requests: owner requests withdrawal from their wallet
CREATE TABLE payout_requests (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id           UUID        NOT NULL REFERENCES tenants(id),
    amount              NUMERIC(19, 4) NOT NULL,
    account_type        VARCHAR(10) NOT NULL CHECK (account_type IN ('BANK', 'UPI')),
    account_holder_name VARCHAR(200),
    account_number      VARCHAR(50),
    ifsc_code           VARCHAR(20),
    upi_id              VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    razorpay_payout_id  VARCHAR(100),
    failure_reason      TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX idx_payout_requests_tenant ON payout_requests(tenant_id);
