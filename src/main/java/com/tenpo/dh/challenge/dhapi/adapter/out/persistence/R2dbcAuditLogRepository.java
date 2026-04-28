package com.tenpo.dh.challenge.dhapi.adapter.out.persistence;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class R2dbcAuditLogRepository implements AuditLogRepository {

    private final AuditLogR2dbcDao dao;

    @Override
    public Mono<AuditLog> save(AuditLog auditLog) {
        return dao.save(auditLog);
    }

    @Override
    public Mono<Page<AuditLog>> findAll(Pageable pageable) {
        return dao.count()
                .flatMap(total -> dao.findAllBy(pageable)
                        .collectList()
                        .map(items -> (Page<AuditLog>) new PageImpl<>(items, pageable, total)));
    }
}
