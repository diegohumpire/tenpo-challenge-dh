package com.tenpo.dh.challenge.dhapi.domain.model;

/**
 * Lightweight context carrying the traceability identifiers for the current request.
 * Propagated through the Reactor context by {@code ExchangeContextFilter} so that
 * application services can stamp audit events without coupling to the web layer.
 */
public record AuditRequestContext(String transactionalId, String userId) {

    public static AuditRequestContext empty() {
        return new AuditRequestContext(null, null);
    }
}
