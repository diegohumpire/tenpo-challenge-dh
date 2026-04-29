package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

/**
 * Snapshot of all data captured from a {@link org.springframework.web.server.ServerWebExchange}
 * needed to produce an {@link com.tenpo.dh.challenge.dhapi.domain.model.AuditLog} entry.
 *
 * <p>Separates the extraction concern (handled by the filter) from the
 * mapping concern (handled by {@link WebExchangeAuditLogMapper}).
 */
public record WebExchangeAuditContext(
        String transactionalId,
        String userId,
        String path,
        String method,
        String params,
        String requestHeaders,
        String requestBody,
        Integer statusCode,
        long durationMs) {}
