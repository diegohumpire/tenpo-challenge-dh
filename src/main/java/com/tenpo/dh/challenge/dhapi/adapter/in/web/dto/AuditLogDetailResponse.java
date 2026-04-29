package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Full detail of an audit log entry including HTTP headers and bodies")
public record AuditLogDetailResponse(
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
