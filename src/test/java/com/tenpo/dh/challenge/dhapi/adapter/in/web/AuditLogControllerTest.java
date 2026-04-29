package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogDetailResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogSummaryResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.LinkDto;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.mapper.AuditLogResponseMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLogFilter;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationResult;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogUseCase auditLogUseCase;

    @Mock
    private AuditLogResponseMapper auditLogResponseMapper;

    @InjectMocks
    private AuditLogController controller;

    @Test
    void getAuditLogs_returnsPagedResponseWithMappedEntries() {
        AuditLog log = AuditLog.builder().id(1L).action("TEST").build();
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(log), 0, 20, 1L, 1);
        AuditLogSummaryResponse dto = new AuditLogSummaryResponse(
                1L, null, "TEST", null, null, null, null,
                null, null, null, null, null, null,
                Map.of("detail", new LinkDto("/api/v1/audit-logs/1")));

        when(auditLogUseCase.findAll(any(PaginationRequest.class), any(AuditLogFilter.class)))
                .thenReturn(Mono.just(result));
        when(auditLogResponseMapper.toSummaryResponse(any(AuditLog.class), any())).thenReturn(dto);

        StepVerifier.create(controller.getAuditLogs(0, 20, "createdAt,desc", null, null, null, null, null))
                .assertNext(resp -> {
                    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(resp.getBody()).isNotNull();
                    assertThat(resp.getBody().content()).hasSize(1);
                    assertThat(resp.getBody().totalElements()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void getAuditLogs_sizeExceedsMax_capsAtHundred() {
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(), 0, 100, 0L, 0);
        when(auditLogUseCase.findAll(any(PaginationRequest.class), any(AuditLogFilter.class)))
                .thenReturn(Mono.just(result));

        StepVerifier.create(controller.getAuditLogs(0, 999, "createdAt,desc", null, null, null, null, null))
                .assertNext(resp -> assertThat(resp.getBody()).isNotNull())
                .verifyComplete();

        verify(auditLogUseCase).findAll(argThat(p -> p.size() == 100), any(AuditLogFilter.class));
    }

    @Test
    void getAuditLogs_withAscSortParam_appliesAscDirection() {
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(), 0, 20, 0L, 0);
        when(auditLogUseCase.findAll(any(PaginationRequest.class), any(AuditLogFilter.class)))
                .thenReturn(Mono.just(result));

        StepVerifier.create(controller.getAuditLogs(0, 20, "action,asc", null, null, null, null, null))
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(auditLogUseCase).findAll(argThat(p ->
                p.sortField().equals("action") &&
                p.sortDirection() == PaginationRequest.SortDirection.ASC), any(AuditLogFilter.class));
    }

    @Test
    void getAuditLogs_withNullSortParam_fallsBackToDefaultCreatedAtDesc() {
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(), 0, 20, 0L, 0);
        when(auditLogUseCase.findAll(any(PaginationRequest.class), any(AuditLogFilter.class)))
                .thenReturn(Mono.just(result));

        StepVerifier.create(controller.getAuditLogs(0, 20, null, null, null, null, null, null))
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(auditLogUseCase).findAll(argThat(p ->
                p.sortField().equals("createdAt") &&
                p.sortDirection() == PaginationRequest.SortDirection.DESC), any(AuditLogFilter.class));
    }

    @Test
    void getAuditLogs_withUserId_passesFilterToUseCase() {
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(), 0, 20, 0L, 0);
        when(auditLogUseCase.findAll(any(PaginationRequest.class), any(AuditLogFilter.class)))
                .thenReturn(Mono.just(result));

        StepVerifier.create(controller.getAuditLogs(0, 20, "createdAt,desc", "user-1", null, null, null, null))
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(auditLogUseCase).findAll(any(), argThat(f -> "user-1".equals(f.userId())));
    }

    @Test
    void getAuditLogById_found_returnsDetail() {
        AuditLog log = AuditLog.builder().id(42L).action("TEST").build();
        AuditLogDetailResponse detail = new AuditLogDetailResponse(
                42L, null, "TEST", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        when(auditLogUseCase.findById(42L)).thenReturn(Mono.just(log));
        when(auditLogResponseMapper.toDetailResponse(log)).thenReturn(detail);

        StepVerifier.create(controller.getAuditLogById(42L))
                .assertNext(resp -> {
                    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(resp.getBody()).isNotNull();
                    assertThat(resp.getBody().id()).isEqualTo(42L);
                })
                .verifyComplete();
    }

    @Test
    void getAuditLogById_notFound_returns404() {
        when(auditLogUseCase.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(controller.getAuditLogById(99L))
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void getByTransactionalId_returnsPagedDetailResponse() {
        AuditLog log = AuditLog.builder().id(1L).build();
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(log), 0, 20, 1L, 1);
        AuditLogDetailResponse detail = new AuditLogDetailResponse(
                1L, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        when(auditLogUseCase.findAll(any(PaginationRequest.class), any(AuditLogFilter.class)))
                .thenReturn(Mono.just(result));
        when(auditLogResponseMapper.toDetailResponse(any())).thenReturn(detail);

        StepVerifier.create(controller.getByTransactionalId("txn-123", 0, 20, "createdAt,asc"))
                .assertNext(resp -> {
                    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(resp.getBody().totalElements()).isEqualTo(1);
                })
                .verifyComplete();

        verify(auditLogUseCase).findAll(any(),
                argThat(f -> "txn-123".equals(f.transactionalId()) && f.excludeActions().contains("GET_AUDIT_LOGS")));
    }

    @Test
    void getByUserId_returnsPagedDetailResponse() {
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(), 0, 20, 0L, 0);
        when(auditLogUseCase.findAll(any(PaginationRequest.class), any(AuditLogFilter.class)))
                .thenReturn(Mono.just(result));

        StepVerifier.create(controller.getByUserId("user-456", 0, 20, "createdAt,asc"))
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(auditLogUseCase).findAll(any(),
                argThat(f -> "user-456".equals(f.userId()) && f.excludeActions().contains("GET_AUDIT_LOGS")));
    }
}
