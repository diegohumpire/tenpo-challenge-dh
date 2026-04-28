package com.tenpo.dh.challenge.dhapi.domain.port.out;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface AuditLogRepository {
    Mono<AuditLog> save(AuditLog auditLog);
    Mono<Page<AuditLog>> findAll(Pageable pageable);
}
