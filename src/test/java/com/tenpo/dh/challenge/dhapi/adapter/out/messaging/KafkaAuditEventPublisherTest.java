package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaAuditEventPublisherTest {

    private static final String TOPIC = "audit-events";

    @Mock
    private KafkaSender<String, String> kafkaSender;

    private KafkaAuditEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaAuditEventPublisher(kafkaSender, new ObjectMapper().findAndRegisterModules(), TOPIC);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publish_validAuditLog_sendsJsonToKafkaTopic() throws InterruptedException {
        doReturn(Flux.empty()).when(kafkaSender).send(any());

        publisher.publish(buildAuditLog());

        Thread.sleep(200);
        verify(kafkaSender, times(1)).send(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publish_kafkaSendFails_doesNotPropagateException() {
        doReturn(Flux.error(new RuntimeException("Kafka down"))).when(kafkaSender).send(any());

        assertThatNoException().isThrownBy(() -> publisher.publish(buildAuditLog()));
    }

    @Test
    void publish_serializationFails_doesNotCallKafka() throws Exception {
        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("forced") {});
        KafkaAuditEventPublisher brokenPublisher =
                new KafkaAuditEventPublisher(kafkaSender, brokenMapper, TOPIC);

        assertThatNoException().isThrownBy(() -> brokenPublisher.publish(buildAuditLog()));
        verifyNoInteractions(kafkaSender);
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
