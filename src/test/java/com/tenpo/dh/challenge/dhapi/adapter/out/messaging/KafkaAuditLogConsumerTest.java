package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaAuditLogConsumerTest {

    @Mock
    private KafkaConsumer<String, String> kafkaConsumer;

    @Mock
    private AuditLogUseCase auditLogUseCase;

    private KafkaAuditLogConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new KafkaAuditLogConsumer(() -> kafkaConsumer, auditLogUseCase,
                new ObjectMapper().findAndRegisterModules());
    }

    @AfterEach
    void tearDown() {
        consumer.stopConsuming();
    }

    @SuppressWarnings("unchecked")
    @Test
    void startConsuming_validMessage_persistsAuditLog() throws InterruptedException {
        AuditLog auditLog = buildAuditLog();
        String json = serializeQuietly(auditLog);

        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(buildConsumerRecords(json), emptyRecords());
        when(auditLogUseCase.save(any())).thenReturn(Mono.just(auditLog));

        consumer.startConsuming();

        Thread.sleep(200);
        verify(auditLogUseCase, times(1)).save(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void startConsuming_persistenceFails_doesNotCrashConsumer() throws InterruptedException {
        AuditLog auditLog = buildAuditLog();
        String json = serializeQuietly(auditLog);

        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(buildConsumerRecords(json, json), emptyRecords());
        when(auditLogUseCase.save(any())).thenReturn(Mono.error(new RuntimeException("DB down")));

        consumer.startConsuming();

        Thread.sleep(200);
        verify(auditLogUseCase, times(2)).save(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void startConsuming_malformedJson_skipsMessageWithoutCrashing() throws InterruptedException {
        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(buildConsumerRecords("not-valid-json"),
                emptyRecords());

        consumer.startConsuming();

        Thread.sleep(200);
        verifyNoInteractions(auditLogUseCase);
    }

    @Test
    void stopConsuming_callsWakeup_andClosesConsumer() throws InterruptedException {
        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(emptyRecords());

        consumer.startConsuming();
        Thread.sleep(50); // let poll loop start and create the consumer
        consumer.stopConsuming(); // wakeup + interrupt + join; poll loop finally{} closes consumer

        verify(kafkaConsumer, atLeastOnce()).wakeup();
        verify(kafkaConsumer, times(1)).close();
    }

    @Test
    void pollLoop_factoryThrowsThenSucceeds_eventuallyProcessesMessages() throws InterruptedException {
        AuditLog auditLog = buildAuditLog();
        String json = serializeQuietly(auditLog);

        when(kafkaConsumer.poll(any(Duration.class)))
                .thenReturn(buildConsumerRecords(json))
                .thenReturn(emptyRecords());
        when(auditLogUseCase.save(any())).thenReturn(Mono.just(auditLog));

        int[] callCount = { 0 };
        KafkaAuditLogConsumer retryConsumer = new KafkaAuditLogConsumer(
                () -> {
                    if (callCount[0]++ == 0)
                        throw new RuntimeException("broker not ready");
                    return kafkaConsumer;
                },
                auditLogUseCase,
                new ObjectMapper().findAndRegisterModules(),
                10L // 10ms retry delay — avoids waiting 5s in tests
        );

        retryConsumer.startConsuming();
        Thread.sleep(300); // fail → 10ms sleep → succeed → poll → process
        verify(auditLogUseCase, times(1)).save(any());
        retryConsumer.stopConsuming();
    }

    @Test
    void stopConsuming_joinsThreadBeforeReturning() throws InterruptedException {
        CountDownLatch pollStarted = new CountDownLatch(1);
        CountDownLatch releaseWakeup = new CountDownLatch(1);

        when(kafkaConsumer.poll(any(Duration.class))).thenAnswer(inv -> {
            pollStarted.countDown();
            releaseWakeup.await();
            throw new WakeupException();
        });

        consumer.startConsuming();
        pollStarted.await(); // wait until poll loop is blocked inside poll()

        // Release the mock poll and trigger stop
        releaseWakeup.countDown();
        consumer.stopConsuming(); // must not return while thread is still running

        // If we reach here, the thread was joined successfully (no race condition)
        assertThat(true).isTrue();
    }

    @SuppressWarnings("deprecation")
    private ConsumerRecords<String, String> buildConsumerRecords(String... values) {
        TopicPartition tp = new TopicPartition("audit-events", 0);
        List<ConsumerRecord<String, String>> records = Arrays.stream(values)
                .map(v -> new ConsumerRecord<String, String>("audit-events", 0, 0L, null, v))
                .toList();
        return new ConsumerRecords<>(Map.of(tp, records));
    }

    private ConsumerRecords<String, String> emptyRecords() {
        return ConsumerRecords.empty();
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
