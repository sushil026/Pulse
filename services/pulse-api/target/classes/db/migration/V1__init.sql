CREATE TABLE tenants (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE apps (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE api_keys (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id     UUID NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    public_key VARCHAR(11) NOT NULL UNIQUE,
    secret_key VARCHAR(11) NOT NULL UNIQUE,
    active     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE notification_templates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id           UUID NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    template_id      VARCHAR(100) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    type             VARCHAR(50) NOT NULL,
    primary_channel  VARCHAR(50) NOT NULL,
    fallback_channel VARCHAR(50),
    definition       JSONB NOT NULL,
    retry_config     JSONB,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW(),
    UNIQUE (app_id, template_id)
);

CREATE TABLE notification_instances (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id        UUID NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    template_id   VARCHAR(100) NOT NULL,
    instance_id   VARCHAR(255) NOT NULL,
    user_id       VARCHAR(255) NOT NULL,
    current_state VARCHAR(100),
    payload       JSONB,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    UNIQUE (app_id, instance_id)
);

CREATE TABLE state_transitions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id  UUID NOT NULL REFERENCES notification_instances(id) ON DELETE CASCADE,
    from_state   VARCHAR(100),
    to_state     VARCHAR(100) NOT NULL,
    event        VARCHAR(100) NOT NULL,
    payload      JSONB,
    triggered_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE delivery_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id  UUID NOT NULL REFERENCES notification_instances(id) ON DELETE CASCADE,
    channel      VARCHAR(50) NOT NULL,
    status       VARCHAR(50) NOT NULL,
    attempt      INT DEFAULT 1,
    error        TEXT,
    delivered_at TIMESTAMP
);

CREATE TABLE device_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id     UUID NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    user_id    VARCHAR(255) NOT NULL,
    token      VARCHAR(500) NOT NULL,
    platform   VARCHAR(20) NOT NULL,
    active     BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE (app_id, user_id, platform)
);

CREATE TABLE outbox (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB NOT NULL,
    published    BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE scheduled_jobs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id      UUID NOT NULL REFERENCES apps(id) ON DELETE CASCADE,
    template_id VARCHAR(100) NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    payload     JSONB,
    deliver_at  TIMESTAMP NOT NULL,
    status      VARCHAR(50) DEFAULT 'PENDING',
    created_at  TIMESTAMP DEFAULT NOW()
);

-- apps
CREATE INDEX idx_apps_tenant_id ON apps (tenant_id);

-- api_keys
CREATE INDEX idx_api_keys_app_id ON api_keys (app_id);
CREATE INDEX idx_api_keys_public_key ON api_keys (public_key);

-- notification_templates
CREATE INDEX idx_notification_templates_app_id ON notification_templates (app_id);

-- notification_instances
CREATE INDEX idx_notification_instances_app_id ON notification_instances (app_id);
CREATE INDEX idx_notification_instances_user_id ON notification_instances (user_id);
CREATE INDEX idx_notification_instances_instance_id ON notification_instances (instance_id);

-- state_transitions
CREATE INDEX idx_state_transitions_instance_id ON state_transitions (instance_id);

-- delivery_logs
CREATE INDEX idx_delivery_logs_instance_id ON delivery_logs (instance_id);

-- device_tokens
CREATE INDEX idx_device_tokens_app_id_user_id ON device_tokens (app_id, user_id);

-- outbox
CREATE INDEX idx_outbox_published_created_at ON outbox (published, created_at);

-- scheduled_jobs
CREATE INDEX idx_scheduled_jobs_deliver_at_status ON scheduled_jobs (deliver_at, status);

-- auto-update updated_at on row modification
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_notification_templates_updated_at
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_notification_instances_updated_at
    BEFORE UPDATE ON notification_instances
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
