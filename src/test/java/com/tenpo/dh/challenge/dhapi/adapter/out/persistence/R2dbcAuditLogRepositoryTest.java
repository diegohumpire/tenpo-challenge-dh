package com.tenpo.dh.challenge.dhapi.adapter.out.persistence;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLogFilter;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveSelectOperation;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class R2dbcAuditLogRepositoryTest {

    @Mock
    private AuditLogR2dbcDao dao;

    @Mock
    private AuditLogEntityMapper entityMapper;

    @Mock
    private R2dbcEntityTemplate template;

    @InjectMocks
    private R2dbcAuditLogRepository repository;

    @Test
    void save_delegatesToDaoAndMapsResult() {
        AuditLog domain = AuditLog.builder().id(1L).action("TEST").build();
        AuditLogEntity entity = AuditLogEntity.builder().id(1L).action("TEST").build();

        when(entityMapper.toEntity(domain)).thenReturn(entity);
        when(dao.save(entity)).thenReturn(Mono.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(repository.save(domain))
                .expectNext(domain)
                .verifyComplete();
    }

    @Test
    void findById_delegatesToDaoAndMapsResult() {
        AuditLog domain = AuditLog.builder().id(42L).action("TEST").build();
        AuditLogEntity entity = AuditLogEntity.builder().id(42L).action("TEST").build();

        when(dao.findById(42L)).thenReturn(Mono.just(entity));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(repository.findById(42L))
                .expectNext(domain)
                .verifyComplete();
    }

    @Test
    void findAll_emptyFilter_usesDaoPath_returnsPaginatedResultsWithCorrectMetadata() {
        AuditLog domain = AuditLog.builder().id(1L).action("TEST").build();
        AuditLogEntity entity = AuditLogEntity.builder().id(1L).action("TEST").build();
        PaginationRequest request = new PaginationRequest(0, 10, "createdAt", PaginationRequest.SortDirection.DESC);
        PageRequest expectedPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(dao.count()).thenReturn(Mono.just(100L));
        when(dao.findAllBy(expectedPageable)).thenReturn(Flux.fromIterable(List.of(entity)));
        when(entityMapper.toDomain(any(AuditLogEntity.class))).thenReturn(domain);

        StepVerifier.create(repository.findAll(request, AuditLogFilter.empty()))
                .assertNext(result -> {
                    assertThat(result.totalElements()).isEqualTo(100L);
                    assertThat(result.content()).containsExactly(domain);
                    assertThat(result.size()).isEqualTo(10);
                    assertThat(result.page()).isEqualTo(0);
                    assertThat(result.totalPages()).isEqualTo(10);
                })
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findAll_nonEmptyFilter_usesTemplatePath() {
        AuditLog domain = AuditLog.builder().id(2L).action("CALCULATE").build();
        AuditLogEntity entity = AuditLogEntity.builder().id(2L).action("CALCULATE").build();
        PaginationRequest request = new PaginationRequest(0, 10, "createdAt", PaginationRequest.SortDirection.DESC);
        AuditLogFilter filter = AuditLogFilter.forUserId("user-1");

        ReactiveSelectOperation.ReactiveSelect<AuditLogEntity> selectOp =
                mock(ReactiveSelectOperation.ReactiveSelect.class);
        ReactiveSelectOperation.TerminatingSelect<AuditLogEntity> termOp =
                mock(ReactiveSelectOperation.TerminatingSelect.class);

        when(template.count(any(Query.class), eq(AuditLogEntity.class))).thenReturn(Mono.just(1L));
        when(template.select(AuditLogEntity.class)).thenReturn(selectOp);
        when(selectOp.matching(any(Query.class))).thenReturn(termOp);
        when(termOp.all()).thenReturn(Flux.fromIterable(List.of(entity)));
        when(entityMapper.toDomain(entity)).thenReturn(domain);

        StepVerifier.create(repository.findAll(request, filter))
                .assertNext(result -> {
                    assertThat(result.totalElements()).isEqualTo(1L);
                    assertThat(result.content()).containsExactly(domain);
                })
                .verifyComplete();
    }

    @SuppressWarnings("unchecked")
    @Test
    void findAll_filterWithAllFields_buildsCriteriaWithoutError() {
        PaginationRequest request = new PaginationRequest(0, 5, "createdAt", PaginationRequest.SortDirection.ASC);
        AuditLogFilter filter = new AuditLogFilter(
                "user-1", "txn-1", "CALCULATE",
                AuditActionType.SYSTEM, null, List.of("GET_AUDIT_LOGS"));

        ReactiveSelectOperation.ReactiveSelect<AuditLogEntity> selectOp =
                mock(ReactiveSelectOperation.ReactiveSelect.class);
        ReactiveSelectOperation.TerminatingSelect<AuditLogEntity> termOp =
                mock(ReactiveSelectOperation.TerminatingSelect.class);

        when(template.count(any(Query.class), eq(AuditLogEntity.class))).thenReturn(Mono.just(0L));
        when(template.select(AuditLogEntity.class)).thenReturn(selectOp);
        when(selectOp.matching(any(Query.class))).thenReturn(termOp);
        when(termOp.all()).thenReturn(Flux.empty());

        StepVerifier.create(repository.findAll(request, filter))
                .assertNext(result -> assertThat(result.totalElements()).isZero())
                .verifyComplete();
    }
}
