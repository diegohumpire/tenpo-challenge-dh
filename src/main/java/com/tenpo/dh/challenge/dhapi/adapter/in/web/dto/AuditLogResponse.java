package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long id,
        OffsetDateTime createdAt,
        String action,
        AuditActionType actionType,
        CallDirection callDirection,
        String userId,
        String transactionalId,
        String method,
        String endpoint,
        String params,
        String requestHeaders,
        String requestBody,
        String responseHeaders,
        String responseBody,
        Integer statusCode,
        String errorMessage,
        Long durationMs
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getCreatedAt(),
                log.getAction(),
                log.getActionType(),
                log.getCallDirection(),
                log.getUserId(),
                log.getTransactionalId(),
                log.getMethod(),
                log.getEndpoint(),
                log.getParams(),
                log.getRequestHeaders(),
                log.getRequestBody(),
                log.getResponseHeaders(),
                log.getResponseBody(),
                log.getStatusCode(),
                log.getErrorMessage(),
                log.getDurationMs()
        );
    }
}
