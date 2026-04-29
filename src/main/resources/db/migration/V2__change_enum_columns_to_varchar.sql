-- Spring Data R2DBC binds Java enums as character varying; native PG enum types
-- reject that binding. Storing as VARCHAR avoids the codec mismatch while keeping
-- the Java-level type safety provided by AuditActionType / CallDirection enums.

ALTER TABLE audit_logs
    ALTER COLUMN action_type    TYPE VARCHAR(50) USING action_type::text,
    ALTER COLUMN call_direction TYPE VARCHAR(10)  USING call_direction::text;

DROP TYPE audit_action_type;
DROP TYPE call_direction;
