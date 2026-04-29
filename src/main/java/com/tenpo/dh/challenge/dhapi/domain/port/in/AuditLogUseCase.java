package com.tenpo.dh.challenge.dhapi.domain.port.in;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationResult;
import reactor.core.publisher.Mono;

public interface AuditLogUseCase {
    Mono<AuditLog> save(AuditLog auditLog);
    Mono<PaginationResult<AuditLog>> findAll(PaginationRequest request);
}
