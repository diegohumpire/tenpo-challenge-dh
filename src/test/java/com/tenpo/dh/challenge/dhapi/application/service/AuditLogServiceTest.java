package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
        Page<AuditLog> page = new PageImpl<>(List.of());
        PageRequest pageable = PageRequest.of(0, 20);
        when(auditLogRepository.findAll(pageable)).thenReturn(Mono.just(page));

        StepVerifier.create(auditLogService.findAll(pageable))
                .expectNext(page)
                .verifyComplete();

        verify(auditLogRepository).findAll(pageable);
    }
}
