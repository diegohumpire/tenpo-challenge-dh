package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService implements AuditLogUseCase {

    private final AuditLogRepository auditLogRepository;

    @Override
    public Mono<AuditLog> save(AuditLog auditLog) {
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(OffsetDateTime.now());
        }
        return auditLogRepository.save(auditLog)
                .doOnError(e -> log.error("Failed to save audit log: {}", e.getMessage()));
    }

    @Override
    public Mono<Page<AuditLog>> findAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
