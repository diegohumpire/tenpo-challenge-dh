package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;

/**
 * Kafka consumer that drains the audit-events topic and persists each entry via
 * {@link AuditLogUseCase}.
 *
 * Only active when {@code audit.publisher=kafka}. Mirrors the subscription pattern
 * used by {@link SinkAuditEventPublisher} for the in-memory mode.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "audit.publisher", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaAuditLogConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final AuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;

    private Disposable subscription;

    @PostConstruct
    public void startConsuming() {
        subscription = kafkaReceiver.receive()
                .flatMap(record -> {
                    record.receiverOffset().acknowledge();
                    return deserialize(record.value())
                            .flatMap(auditLogUseCase::save)
                            .onErrorResume(e -> {
                                log.error("Failed to persist audit log from Kafka: {}", e.getMessage());
                                return Mono.empty();
                            });
                })
                .subscribe();
    }

    @PreDestroy
    public void stopConsuming() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }
    }

    private Mono<AuditLog> deserialize(String json) {
        try {
            return Mono.just(objectMapper.readValue(json, AuditLog.class));
        } catch (Exception e) {
            log.error("Failed to deserialize AuditLog from Kafka message: {}", e.getMessage());
            return Mono.empty();
        }
    }
}
