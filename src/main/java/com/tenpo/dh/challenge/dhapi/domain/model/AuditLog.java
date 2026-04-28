package com.tenpo.dh.challenge.dhapi.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_logs")
public class AuditLog {

    @Id
    private Long id;

    @Column("created_at")
    private OffsetDateTime createdAt;

    private String action;

    @Column("action_type")
    private AuditActionType actionType;

    @Column("call_direction")
    private CallDirection callDirection;

    @Column("user_id")
    private String userId;

    @Column("transactional_id")
    private String transactionalId;

    private String method;
    private String endpoint;
    private String params;

    @Column("request_headers")
    private String requestHeaders;

    @Column("request_body")
    private String requestBody;

    @Column("response_headers")
    private String responseHeaders;

    @Column("response_body")
    private String responseBody;

    @Column("status_code")
    private Integer statusCode;

    @Column("error_message")
    private String errorMessage;

    @Column("duration_ms")
    private Long durationMs;
}
