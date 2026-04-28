-- ENUMs
CREATE TYPE audit_action_type AS ENUM (
    'HTTP_REQUEST',
    'CALCULATION',
    'EXTERNAL_CALL',
    'CACHE_ACCESS',
    'SYSTEM'
);

CREATE TYPE call_direction AS ENUM ('IN', 'OUT');

-- Audit Logs
CREATE TABLE audit_logs (
    id                BIGSERIAL PRIMARY KEY,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Acción auditada
    action            VARCHAR(100) NOT NULL,
    action_type       audit_action_type NOT NULL,

    -- Dirección de la llamada (NULL = acción interna del sistema)
    call_direction    call_direction NULL,

    -- Trazabilidad
    user_id           VARCHAR(100) NULL,
    transactional_id  VARCHAR(100) NULL,

    -- HTTP
    method            VARCHAR(10) NULL,
    endpoint          VARCHAR(512) NULL,
    params            TEXT NULL,
    request_headers   TEXT NULL,
    request_body      TEXT NULL,
    response_headers  TEXT NULL,
    response_body     TEXT NULL,
    status_code       INTEGER NULL,
    error_message     TEXT NULL,
    duration_ms       BIGINT NULL
);

CREATE INDEX idx_audit_logs_created_at     ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_action         ON audit_logs(action);
CREATE INDEX idx_audit_logs_action_type    ON audit_logs(action_type);
CREATE INDEX idx_audit_logs_call_direction ON audit_logs(call_direction);
CREATE INDEX idx_audit_logs_user_id        ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_transactional  ON audit_logs(transactional_id);
