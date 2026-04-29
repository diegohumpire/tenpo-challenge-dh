package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
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
) {}
