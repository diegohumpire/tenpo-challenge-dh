package com.tenpo.dh.challenge.dhapi.domain.port.out;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLogFilter;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationResult;
import reactor.core.publisher.Mono;

public interface AuditLogRepository {
    Mono<AuditLog> save(AuditLog auditLog);
    Mono<AuditLog> findById(Long id);
    Mono<PaginationResult<AuditLog>> findAll(PaginationRequest request, AuditLogFilter filter);
}
