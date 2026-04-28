package com.tenpo.dh.challenge.dhapi.domain.port.in;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface AuditLogUseCase {
    Mono<AuditLog> save(AuditLog auditLog);
    Mono<org.springframework.data.domain.Page<AuditLog>> findAll(Pageable pageable);
}
