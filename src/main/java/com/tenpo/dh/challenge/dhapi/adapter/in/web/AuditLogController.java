package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogResponseMapper;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.PageResponse;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final AuditLogUseCase auditLogUseCase;
    private final AuditLogResponseMapper auditLogResponseMapper;

    @Operation(summary = "Get paginated audit log history", description = "Returns a paginated list of all API calls recorded in the system")
    @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully")
    @GetMapping
    public Mono<ResponseEntity<PageResponse<AuditLogResponse>>> getAuditLogs(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max " + MAX_PAGE_SIZE + ")") @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Sort field and direction") @RequestParam(defaultValue = "createdAt,desc") String sort) {

        int cappedSize = Math.min(size, MAX_PAGE_SIZE);
        Sort sortObj = buildSort(sort);
        PageRequest pageable = PageRequest.of(page, cappedSize, sortObj);

        return auditLogUseCase.findAll(pageable)
                .map(p -> p.map(auditLogResponseMapper::toResponse))
                .map(PageResponse::from)
                .map(ResponseEntity::ok);
    }

    private Sort buildSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0].trim();
            Sort.Direction direction = parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            return Sort.by(direction, field);
        } catch (Exception e) {
            log.warn("Invalid sort parameter '{}', using default createdAt,desc", sort);
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }
}
