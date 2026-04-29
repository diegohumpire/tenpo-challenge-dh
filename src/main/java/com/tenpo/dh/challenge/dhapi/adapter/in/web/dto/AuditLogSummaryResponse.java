package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

@Schema(description = "Lightweight summary of an audit log entry")
public record AuditLogSummaryResponse(
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
        Integer statusCode,
        String errorMessage,
        Long durationMs,
        @JsonProperty("_links") Map<String, LinkDto> links
) {}
