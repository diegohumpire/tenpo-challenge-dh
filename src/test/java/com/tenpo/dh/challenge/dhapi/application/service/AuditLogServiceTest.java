package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationRequest;
import com.tenpo.dh.challenge.dhapi.domain.model.PaginationResult;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void save_withCreatedAtNull_setsCreatedAtBeforePersisting() {
        AuditLog log = AuditLog.builder().action("TEST").build();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(Mono.just(log));

        StepVerifier.create(auditLogService.save(log))
                .expectNext(log)
                .verifyComplete();

        assertThat(log.getCreatedAt()).isNotNull();
    }

    @Test
    void save_withCreatedAtAlreadySet_doesNotOverrideIt() {
        OffsetDateTime original = OffsetDateTime.parse("2024-01-01T00:00:00Z");
        AuditLog log = AuditLog.builder().action("TEST").createdAt(original).build();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(Mono.just(log));

        StepVerifier.create(auditLogService.save(log))
                .expectNext(log)
                .verifyComplete();

        assertThat(log.getCreatedAt()).isEqualTo(original);
    }

    @Test
    void save_repositoryError_propagatesError() {
        AuditLog log = AuditLog.builder().action("TEST").build();
        when(auditLogRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB error")));

        StepVerifier.create(auditLogService.save(log))
                .expectErrorMessage("DB error")
                .verify();
    }

    @Test
    void findAll_delegatesToRepository() {
        PaginationRequest request = new PaginationRequest(0, 20, "createdAt", PaginationRequest.SortDirection.DESC);
        PaginationResult<AuditLog> result = new PaginationResult<>(List.of(), 0, 20, 0L, 0);
        when(auditLogRepository.findAll(any(PaginationRequest.class), any())).thenReturn(Mono.just(result));

        StepVerifier.create(auditLogService.findAll(request))
                .expectNext(result)
                .verifyComplete();

        verify(auditLogRepository).findAll(any(PaginationRequest.class), any());
    }

    @Test
    void findById_delegatesToRepository() {
        AuditLog log = AuditLog.builder().id(1L).action("TEST").build();
        when(auditLogRepository.findById(1L)).thenReturn(Mono.just(log));

        StepVerifier.create(auditLogService.findById(1L))
                .expectNext(log)
                .verifyComplete();
    }
}
