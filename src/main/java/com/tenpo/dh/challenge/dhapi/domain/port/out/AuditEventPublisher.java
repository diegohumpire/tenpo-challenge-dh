package com.tenpo.dh.challenge.dhapi.domain.port.out;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;

/**
 * Port for publishing audit events.
 * Default implementation persists directly; swap with a Kafka adapter to decouple
 * the HTTP layer from the persistence layer.
 */
public interface AuditEventPublisher {
    void publish(AuditLog auditLog);
}
