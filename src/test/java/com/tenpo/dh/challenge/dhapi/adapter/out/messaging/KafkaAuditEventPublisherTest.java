package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaAuditEventPublisherTest {

    private static final String TOPIC = "audit-events";

    @Mock
    private KafkaProducer<String, String> kafkaProducer;

    private KafkaAuditEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaAuditEventPublisher(() -> kafkaProducer, new ObjectMapper().findAndRegisterModules(),
                TOPIC);
    }

    @Test
    void publish_validAuditLog_sendsJsonToKafkaTopic() throws InterruptedException {
        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(null, null);
            return mock(Future.class);
        }).when(kafkaProducer).send(any(), any());

        publisher.publish(buildAuditLog());

        Thread.sleep(200);
        verify(kafkaProducer, times(1)).send(any(), any());
    }

    @Test
    void publish_kafkaSendFails_doesNotPropagateException() {
        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(null, new RuntimeException("Kafka down"));
            return mock(Future.class);
        }).when(kafkaProducer).send(any(), any());

        assertThatNoException().isThrownBy(() -> publisher.publish(buildAuditLog()));
    }

    @Test
    void publish_serializationFails_doesNotCallKafka() throws Exception {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("forced") {
                });
        KafkaAuditEventPublisher brokenPublisher = new KafkaAuditEventPublisher(() -> kafkaProducer, brokenMapper,
                TOPIC);

        assertThatNoException().isThrownBy(() -> brokenPublisher.publish(buildAuditLog()));
        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void publish_producerFactoryNotCalledAtConstruction() {
        AtomicInteger factoryCalls = new AtomicInteger(0);

        new KafkaAuditEventPublisher(
                () -> {
                    factoryCalls.incrementAndGet();
                    return kafkaProducer;
                },
                new ObjectMapper().findAndRegisterModules(),
                TOPIC);

        assertThat(factoryCalls.get()).isZero();
    }

    @Test
    void publish_firstPublish_callsFactoryExactlyOnce() throws InterruptedException {
        AtomicInteger factoryCalls = new AtomicInteger(0);
        KafkaAuditEventPublisher lazyPublisher = new KafkaAuditEventPublisher(
                () -> {
                    factoryCalls.incrementAndGet();
                    return kafkaProducer;
                },
                new ObjectMapper().findAndRegisterModules(),
                TOPIC);

        doAnswer(inv -> {
            ((Callback) inv.getArgument(1)).onCompletion(null, null);
            return mock(Future.class);
        }).when(kafkaProducer).send(any(), any());

        lazyPublisher.publish(buildAuditLog());
        lazyPublisher.publish(buildAuditLog());
        Thread.sleep(200);

        assertThat(factoryCalls.get()).isEqualTo(1); // created once, reused on second call
    }

    @Test
    void publish_producerFactoryThrows_errorIsSwallowed() {
        KafkaAuditEventPublisher failPublisher = new KafkaAuditEventPublisher(
                () -> {
                    throw new RuntimeException("Broker unavailable");
                },
                new ObjectMapper().findAndRegisterModules(),
                TOPIC);

        assertThatNoException().isThrownBy(() -> failPublisher.publish(buildAuditLog()));
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
