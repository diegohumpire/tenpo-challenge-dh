package com.tenpo.dh.challenge.dhapi.adapter.out.persistence;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AuditLogR2dbcDao extends ReactiveCrudRepository<AuditLog, Long> {
    Flux<AuditLog> findAllBy(Pageable pageable);
    Mono<Long> count();
}
