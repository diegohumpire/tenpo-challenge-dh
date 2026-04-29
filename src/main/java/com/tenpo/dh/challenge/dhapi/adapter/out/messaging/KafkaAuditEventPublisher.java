package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Publishes audit events to a Kafka topic.
 *
 * Each {@link AuditLog} is serialized to JSON and sent as a {@link SenderRecord}.
 * Errors are logged and swallowed so that a Kafka outage never propagates to the HTTP response.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaAuditEventPublisher implements AuditEventPublisher {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;
    private final String topic;

    @Override
    public void publish(AuditLog auditLog) {
        String json = serialize(auditLog);
        if (json == null) {
            return;
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, json);
        kafkaSender.send(reactor.core.publisher.Mono.just(SenderRecord.create(record, null)))
                .doOnError(e -> log.error("Failed to publish audit event to Kafka: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
    }

    private String serialize(AuditLog auditLog) {
        try {
            return objectMapper.writeValueAsString(auditLog);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AuditLog: {}", e.getMessage());
            return null;
        }
    }
}
