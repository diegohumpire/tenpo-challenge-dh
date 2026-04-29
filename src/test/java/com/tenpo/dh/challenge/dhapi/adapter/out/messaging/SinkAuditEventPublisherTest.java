package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SinkAuditEventPublisherTest {

    @Mock
    private AuditLogUseCase auditLogUseCase;

    private SinkAuditEventPublisher publisher;

    @BeforeEach
    void setUp() {
        when(auditLogUseCase.save(any())).thenReturn(Mono.just(AuditLog.builder().build()));
        publisher = new SinkAuditEventPublisher(auditLogUseCase);
    }

    @AfterEach
    void tearDown() {
        publisher.destroy();
    }

    @Test
    void publish_validAuditLog_delegatesToUseCase() throws InterruptedException {
        AuditLog auditLog = buildAuditLog();

        publisher.publish(auditLog);

        Thread.sleep(200);
        verify(auditLogUseCase, times(1)).save(auditLog);
    }

    @Test
    void publish_useCaseThrows_doesNotCrashPublisher() throws InterruptedException {
        when(auditLogUseCase.save(any())).thenReturn(Mono.error(new RuntimeException("DB down")));

        publisher.publish(buildAuditLog());
        publisher.publish(buildAuditLog());

        Thread.sleep(200);
        verify(auditLogUseCase, times(2)).save(any());
    }

    private AuditLog buildAuditLog() {
        return AuditLog.builder()
                .createdAt(OffsetDateTime.now())
                .action("CREATE_CALCULATION")
                .actionType(AuditActionType.CALCULATION)
                .callDirection(CallDirection.IN)
                .method("POST")
                .endpoint("/api/v1/calculations")
                .statusCode(201)
                .build();
    }
}
