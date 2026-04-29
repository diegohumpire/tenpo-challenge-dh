package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaAuditLogConsumerTest {

    @Mock
    private KafkaReceiver<String, String> kafkaReceiver;

    @Mock
    private AuditLogUseCase auditLogUseCase;

    private KafkaAuditLogConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new KafkaAuditLogConsumer(kafkaReceiver, auditLogUseCase, new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void tearDown() {
        consumer.stopConsuming();
    }

    @Test
    void startConsuming_validMessage_persistsAuditLog() throws InterruptedException {
        AuditLog auditLog = buildAuditLog();
        String json = serializeQuietly(auditLog);

        when(kafkaReceiver.receive()).thenReturn(Flux.just(buildReceiverRecord(json)));
        when(auditLogUseCase.save(any())).thenReturn(Mono.just(auditLog));

        consumer.startConsuming();

        Thread.sleep(200);
        verify(auditLogUseCase, times(1)).save(any());
    }

    @Test
    void startConsuming_persistenceFails_doesNotCrashConsumer() throws InterruptedException {
        AuditLog auditLog = buildAuditLog();
        String json = serializeQuietly(auditLog);

        when(kafkaReceiver.receive()).thenReturn(Flux.just(buildReceiverRecord(json), buildReceiverRecord(json)));
        when(auditLogUseCase.save(any())).thenReturn(Mono.error(new RuntimeException("DB down")));

        consumer.startConsuming();

        Thread.sleep(200);
        verify(auditLogUseCase, times(2)).save(any());
    }

    @Test
    void startConsuming_malformedJson_skipsMessageWithoutCrashing() throws InterruptedException {
        when(kafkaReceiver.receive()).thenReturn(Flux.just(buildReceiverRecord("not-valid-json")));

        consumer.startConsuming();

        Thread.sleep(200);
        verifyNoInteractions(auditLogUseCase);
    }

    @SuppressWarnings("unchecked")
    private ReceiverRecord<String, String> buildReceiverRecord(String value) {
        ConsumerRecord<String, String> consumerRecord =
                new ConsumerRecord<>("audit-events", 0, 0L, null, value);
        ReceiverOffset offset = mock(ReceiverOffset.class);
        return new ReceiverRecord<>(consumerRecord, offset);
    }

    private String serializeQuietly(AuditLog auditLog) {
        try {
            return new ObjectMapper().findAndRegisterModules().writeValueAsString(auditLog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
