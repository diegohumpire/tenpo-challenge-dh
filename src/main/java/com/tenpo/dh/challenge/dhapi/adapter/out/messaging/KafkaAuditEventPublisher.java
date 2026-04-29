package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Publishes audit events to a Kafka topic using the native {@link KafkaProducer}.
 *
 * <p>The producer is created <em>lazily</em> on the first {@link #publish} call so
 * that Spring context startup never fails because of an unreachable broker.  If
 * construction fails the factory is retried on the next publish attempt.
 *
 * <p>Each {@link AuditLog} is serialized to JSON and sent asynchronously.  The send
 * callback is wrapped in a {@link Mono} so errors are logged and swallowed — a Kafka
 * outage never propagates to the HTTP response.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaAuditEventPublisher implements AuditEventPublisher {

    private final Supplier<KafkaProducer<String, String>> producerFactory;
    private final ObjectMapper objectMapper;
    private final String topic;

    private volatile KafkaProducer<String, String> producer;

    @Override
    public void publish(AuditLog auditLog) {
        String json = serialize(auditLog);
        if (json == null) {
            return;
        }

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, json);
        Mono.<Void>create(sink ->
                producer().send(record, (metadata, ex) -> {
                    if (ex != null) sink.error(ex);
                    else sink.success(null);
                })
        )
        .doOnError(e -> log.error("Failed to publish audit event to Kafka: {}", e.getMessage()))
        .onErrorComplete()
        .subscribe();
    }

    private KafkaProducer<String, String> producer() {
        if (producer == null) {
            synchronized (this) {
                if (producer == null) {
                    producer = producerFactory.get();
                }
            }
        }
        return producer;
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
