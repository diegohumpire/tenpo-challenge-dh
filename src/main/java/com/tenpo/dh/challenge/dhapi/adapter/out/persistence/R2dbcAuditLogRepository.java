package com.tenpo.dh.challenge.dhapi.adapter.out.persistence;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLogFilter;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationResult;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class R2dbcAuditLogRepository implements AuditLogRepository {

    private final AuditLogR2dbcDao dao;
    private final AuditLogEntityMapper entityMapper;
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<AuditLog> save(AuditLog auditLog) {
        return dao.save(entityMapper.toEntity(auditLog))
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<AuditLog> findById(Long id) {
        return dao.findById(id)
                .map(entityMapper::toDomain);
    }

    @Override
    public Mono<PaginationResult<AuditLog>> findAll(PaginationRequest request, AuditLogFilter filter) {
        Sort sort = request.sortDirection() == PaginationRequest.SortDirection.ASC
                ? Sort.by(Sort.Direction.ASC, request.sortField())
                : Sort.by(Sort.Direction.DESC, request.sortField());
        PageRequest pageable = PageRequest.of(request.page(), request.size(), sort);

        if (filter.isEmpty()) {
            return dao.count()
                    .flatMap(total -> dao.findAllBy(pageable)
                            .map(entityMapper::toDomain)
                            .collectList()
                            .map(items -> toPaginationResult(items, request, total)));
        }

        Criteria criteria = buildCriteria(filter);
        Query query = Query.query(criteria);

        return template.count(query, AuditLogEntity.class)
                .flatMap(total -> template.select(AuditLogEntity.class)
                        .matching(query.with(pageable))
                        .all()
                        .map(entityMapper::toDomain)
                        .collectList()
                        .map(items -> toPaginationResult(items, request, total)));
    }

    private Criteria buildCriteria(AuditLogFilter filter) {
        Criteria criteria = Criteria.empty();
        if (filter.userId() != null) {
            criteria = criteria.and(Criteria.where("user_id").is(filter.userId()));
        }
        if (filter.transactionalId() != null) {
            criteria = criteria.and(Criteria.where("transactional_id").is(filter.transactionalId()));
        }
        if (filter.action() != null) {
            criteria = criteria.and(Criteria.where("action").is(filter.action()));
        }
        if (filter.actionType() != null) {
            criteria = criteria.and(Criteria.where("action_type").is(filter.actionType().name()));
        }
        if (filter.callDirection() != null) {
            criteria = criteria.and(Criteria.where("call_direction").is(filter.callDirection().name()));
        }
        if (!filter.excludeActions().isEmpty()) {
            criteria = criteria.and(Criteria.where("action").notIn(filter.excludeActions()));
        }
        return criteria;
    }

    private PaginationResult<AuditLog> toPaginationResult(
            java.util.List<AuditLog> items, PaginationRequest request, long total) {
        return new PaginationResult<>(
                items,
                request.page(),
                request.size(),
                total,
                request.size() > 0 ? (int) Math.ceil((double) total / request.size()) : 0
        );
    }
}
