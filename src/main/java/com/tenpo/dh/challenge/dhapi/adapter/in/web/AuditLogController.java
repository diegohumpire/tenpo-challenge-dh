package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogDetailResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogSummaryResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.PageResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.mapper.AuditLogResponseMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLogFilter;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(value = "/audit-logs", version = "1")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoint for querying the API call history")
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String BASE_PATH = "/api/v1/audit-logs";

    private final AuditLogUseCase auditLogUseCase;
    private final AuditLogResponseMapper auditLogResponseMapper;

    @Operation(summary = "Get paginated audit log history",
            description = "Returns a lightweight paginated list. Supports optional filters on indexed fields.",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-Transactional-Id", required = true,
                            description = "Correlation ID for distributed tracing, injected by the API Gateway",
                            example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                            description = "Authenticated user identity, injected by the API Gateway",
                            example = "user-123", schema = @Schema(type = "string"))
            })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully", headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID for distributed tracing", schema = @Schema(type = "string")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            }),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)),
                    headers = {
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "0")),
                    @Header(name = "X-RateLimit-Reset", description = "Unix timestamp (seconds) when the rate limit window resets", schema = @Schema(type = "integer", example = "1714000060")),
                    @Header(name = "Retry-After", description = "Seconds to wait before making another request", schema = @Schema(type = "integer", example = "58"))
            })
    })
    @GetMapping
    public Mono<ResponseEntity<PageResponse<AuditLogSummaryResponse>>> getAuditLogs(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max " + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Sort field and direction") @RequestParam(defaultValue = "createdAt,desc") String sort,
            @Parameter(description = "Filter by userId") @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by transactionalId") @RequestParam(required = false) String transactionalId,
            @Parameter(description = "Filter by action") @RequestParam(required = false) String action,
            @Parameter(description = "Filter by actionType") @RequestParam(required = false) AuditActionType actionType,
            @Parameter(description = "Filter by callDirection") @RequestParam(required = false) CallDirection callDirection) {

        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        PaginationRequest request = buildPaginationRequest(page, cappedSize, sort);
        AuditLogFilter filter = new AuditLogFilter(userId, transactionalId, action, actionType, callDirection, java.util.List.of());

        return auditLogUseCase.findAll(request, filter)
                .map(p -> p.map(log -> auditLogResponseMapper.toSummaryResponse(log, BASE_PATH + "/" + log.getId())))
                .map(PageResponse::from)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Get audit log detail",
            description = "Returns all fields for a single audit log entry including HTTP headers and bodies.",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-Transactional-Id", required = true,
                            description = "Correlation ID for distributed tracing, injected by the API Gateway",
                            example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                            description = "Authenticated user identity, injected by the API Gateway",
                            example = "user-123", schema = @Schema(type = "string"))
            })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit log found", headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID for distributed tracing", schema = @Schema(type = "string")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            }),
            @ApiResponse(responseCode = "404", description = "Audit log not found",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)),
                    headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID for distributed tracing", schema = @Schema(type = "string")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            }),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)),
                    headers = {
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "0")),
                    @Header(name = "X-RateLimit-Reset", description = "Unix timestamp (seconds) when the rate limit window resets", schema = @Schema(type = "integer", example = "1714000060")),
                    @Header(name = "Retry-After", description = "Seconds to wait before making another request", schema = @Schema(type = "integer", example = "58"))
            })
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AuditLogDetailResponse>> getAuditLogById(@PathVariable Long id) {
        return auditLogUseCase.findById(id)
                .map(auditLogResponseMapper::toDetailResponse)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get audit logs by transactional ID",
            description = "Returns all fields for a given transactional ID, excluding GET_AUDIT_LOGS entries.",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-Transactional-Id", required = true,
                            description = "Correlation ID for distributed tracing, injected by the API Gateway",
                            example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                            description = "Authenticated user identity, injected by the API Gateway",
                            example = "user-123", schema = @Schema(type = "string"))
            })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully", headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID for distributed tracing", schema = @Schema(type = "string")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            }),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)),
                    headers = {
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "0")),
                    @Header(name = "X-RateLimit-Reset", description = "Unix timestamp (seconds) when the rate limit window resets", schema = @Schema(type = "integer", example = "1714000060")),
                    @Header(name = "Retry-After", description = "Seconds to wait before making another request", schema = @Schema(type = "integer", example = "58"))
            })
    })
    @GetMapping("/transactions/{transactionalId}")
    public Mono<ResponseEntity<PageResponse<AuditLogDetailResponse>>> getByTransactionalId(
            @PathVariable String transactionalId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max " + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Sort field and direction") @RequestParam(defaultValue = "createdAt,asc") String sort) {

        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        PaginationRequest request = buildPaginationRequest(page, cappedSize, sort);

        return auditLogUseCase.findAll(request, AuditLogFilter.forTransactionalId(transactionalId))
                .map(p -> p.map(auditLogResponseMapper::toDetailResponse))
                .map(PageResponse::from)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Get audit logs by user ID",
            description = "Returns all fields for a given user ID, excluding GET_AUDIT_LOGS entries.",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "X-Transactional-Id", required = true,
                            description = "Correlation ID for distributed tracing, injected by the API Gateway",
                            example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true,
                            description = "Authenticated user identity, injected by the API Gateway",
                            example = "user-123", schema = @Schema(type = "string"))
            })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully", headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID for distributed tracing", schema = @Schema(type = "string")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            }),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)),
                    headers = {
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "0")),
                    @Header(name = "X-RateLimit-Reset", description = "Unix timestamp (seconds) when the rate limit window resets", schema = @Schema(type = "integer", example = "1714000060")),
                    @Header(name = "Retry-After", description = "Seconds to wait before making another request", schema = @Schema(type = "integer", example = "58"))
            })
    })
    @GetMapping("/users/{userId}")
    public Mono<ResponseEntity<PageResponse<AuditLogDetailResponse>>> getByUserId(
            @PathVariable String userId,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max " + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Sort field and direction") @RequestParam(defaultValue = "createdAt,asc") String sort) {

        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        PaginationRequest request = buildPaginationRequest(page, cappedSize, sort);

        return auditLogUseCase.findAll(request, AuditLogFilter.forUserId(userId))
                .map(p -> p.map(auditLogResponseMapper::toDetailResponse))
                .map(PageResponse::from)
                .map(ResponseEntity::ok);
    }

    private PaginationRequest buildPaginationRequest(int page, int size, String sort) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            PaginationRequest.SortDirection direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                    ? PaginationRequest.SortDirection.ASC
                    : PaginationRequest.SortDirection.DESC;
            return new PaginationRequest(page, size, field, direction);
        } catch (Exception e) {
            log.warn("Invalid sort parameter '{}', using default createdAt,desc", sort);
            return new PaginationRequest(page, size, "createdAt", PaginationRequest.SortDirection.DESC);
        }
    }
}
