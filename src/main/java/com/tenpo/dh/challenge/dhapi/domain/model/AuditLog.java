package com.tenpo.dh.challenge.dhapi.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    private Long id;
    private OffsetDateTime createdAt;
    private String action;
    private AuditActionType actionType;
    private CallDirection callDirection;
    private String userId;
    private String transactionalId;
    private String method;
    private String endpoint;
    private String params;
    private String requestHeaders;
    private String requestBody;
    private String responseHeaders;
    private String responseBody;
    private Integer statusCode;
    private String errorMessage;
    private Long durationMs;
}
