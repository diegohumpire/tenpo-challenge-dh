package com.tenpo.dh.challenge.dhapi.adapter.out.persistence;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationResult;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class R2dbcAuditLogRepository implements AuditLogRepository {

    private final AuditLogR2dbcDao dao;
    private final AuditLogEntityMapper entityMapper;

    @Override
    public Mono<AuditLog> save(AuditLog auditLog) {
        return dao.save(entityMapper.toEntity(auditLog))
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<PaginationResult<AuditLog>> findAll(PaginationRequest request) {
        Sort sort = request.sortDirection() == PaginationRequest.SortDirection.ASC
                ? Sort.by(Sort.Direction.ASC, request.sortField())
                : Sort.by(Sort.Direction.DESC, request.sortField());
        PageRequest pageable = PageRequest.of(request.page(), request.size(), sort);

        return dao.count()
                .flatMap(total -> dao.findAllBy(pageable)
                        .map(entityMapper::toDomain)
                        .collectList()
                        .map(items -> new PaginationResult<>(
                                items,
                                request.page(),
                                request.size(),
                                total,
                                request.size() > 0 ? (int) Math.ceil((double) total / request.size()) : 0
                        )));
    }
}
