package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogResponse;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.AuditLogResponseMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

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
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);
        AuditLogResponse dto = new AuditLogResponse(1L, null, "TEST", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);

        when(auditLogUseCase.findAll(any())).thenReturn(Mono.just(page));
        when(auditLogResponseMapper.toResponse(any(AuditLog.class))).thenReturn(dto);

        StepVerifier.create(controller.getAuditLogs(0, 20, "createdAt,desc"))
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
        Page<AuditLog> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(auditLogUseCase.findAll(any())).thenReturn(Mono.just(page));

        StepVerifier.create(controller.getAuditLogs(0, 999, "createdAt,desc"))
                .assertNext(resp -> assertThat(resp.getBody()).isNotNull())
                .verifyComplete();

        verify(auditLogUseCase).findAll(argThat(p -> p.getPageSize() == 100));
    }

    @Test
    void getAuditLogs_withAscSortParam_appliesAscDirection() {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogUseCase.findAll(any())).thenReturn(Mono.just(page));

        StepVerifier.create(controller.getAuditLogs(0, 20, "action,asc"))
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(auditLogUseCase).findAll(argThat(p ->
                p.getSort().getOrderFor("action") != null &&
                p.getSort().getOrderFor("action").getDirection() == Sort.Direction.ASC));
    }

    @Test
    void getAuditLogs_withNullSortParam_fallsBackToDefaultCreatedAtDesc() {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogUseCase.findAll(any())).thenReturn(Mono.just(page));

        // null causes NPE in sort.split(",") which is caught and defaults to createdAt,desc
        StepVerifier.create(controller.getAuditLogs(0, 20, null))
                .assertNext(resp -> assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();

        verify(auditLogUseCase).findAll(argThat(p ->
                p.getSort().getOrderFor("createdAt") != null &&
                p.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC));
    }
}
