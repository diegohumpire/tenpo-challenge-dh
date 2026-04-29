package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Kafka consumer that drains the audit-events topic and persists each entry via
 * {@link AuditLogUseCase}.
 *
 * <p>Only active when {@code audit.publisher=kafka}. Runs a poll loop on a dedicated
 * virtual thread. The {@link KafkaConsumer} is created <em>lazily</em> inside the poll
 * loop (not at Spring startup) to avoid eager DNS validation of {@code bootstrap.servers}
 * in kafka-clients 4.x.
 *
 * <p>Shutdown: {@link KafkaConsumer#wakeup()} is called from the {@link PreDestroy} hook,
 * which causes the current/next {@code poll()} to throw {@link WakeupException}, exiting
 * the loop cleanly. The consumer is closed in the loop's {@code finally} block.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "audit.publisher", havingValue = "kafka")
public class KafkaAuditLogConsumer {

    private static final long DEFAULT_RETRY_DELAY_MS = 5_000;

    private final Supplier<KafkaConsumer<String, String>> consumerFactory;
    private final AuditLogUseCase auditLogUseCase;
    private final ObjectMapper objectMapper;
    private final long retryDelayMs;

    @Autowired
    public KafkaAuditLogConsumer(
            Supplier<KafkaConsumer<String, String>> consumerFactory,
            AuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper) {
        this(consumerFactory, auditLogUseCase, objectMapper, DEFAULT_RETRY_DELAY_MS);
    }

    /** Package-private: allows tests to inject a short retry delay without waiting 5 seconds. */
    KafkaAuditLogConsumer(
            Supplier<KafkaConsumer<String, String>> consumerFactory,
            AuditLogUseCase auditLogUseCase,
            ObjectMapper objectMapper,
            long retryDelayMs) {
        this.consumerFactory = consumerFactory;
        this.auditLogUseCase = auditLogUseCase;
        this.objectMapper = objectMapper;
        this.retryDelayMs = retryDelayMs;
    }

    private volatile boolean stopped = false;
    private volatile Thread consumerThread;
    private volatile KafkaConsumer<String, String> kafkaConsumer;

    @PostConstruct
    public void startConsuming() {
        consumerThread = Thread.ofVirtual().name("kafka-audit-consumer").start(this::pollLoop);
    }

    /**
     * Signals the poll loop to stop and blocks until the virtual thread has fully exited.
     * Calls {@link KafkaConsumer#wakeup()} if the consumer is active, and interrupts
     * the thread to break out of any retry sleep immediately.
     */
    @PreDestroy
    public void stopConsuming() {
        stopped = true;
        KafkaConsumer<String, String> consumer = kafkaConsumer;
        if (consumer != null) {
            consumer.wakeup();
        }
        Thread thread = consumerThread;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Outer retry loop: on any non-{@link WakeupException} failure (including broker
     * unreachable at startup), logs the error and retries after {@code retryDelayMs}ms.
     * This prevents the consumer thread from dying silently if Kafka is not yet ready
     * when {@link PostConstruct} fires.
     */
    private void pollLoop() {
        log.info("Kafka audit consumer thread started");
        while (!stopped) {
            try {
                kafkaConsumer = consumerFactory.get();
                log.info("Kafka audit consumer connected to broker");
                try {
                    while (!stopped) {
                        var records = kafkaConsumer.poll(Duration.ofMillis(500));
                        if (!records.isEmpty()) {
                            log.info("Kafka audit consumer: received {} record(s)", records.count());
                            records.forEach(record -> processRecord(record.value()));
                            kafkaConsumer.commitAsync((offsets, ex) -> {
                                if (ex != null) {
                                    log.warn("Kafka audit consumer: offset commit failed — {}", ex.getMessage());
                                }
                            });
                        }
                    }
                } catch (WakeupException e) {
                    log.info("Kafka audit consumer received wakeup — shutting down");
                    return;
                } finally {
                    kafkaConsumer.close();
                    kafkaConsumer = null;
                    log.info("Kafka audit consumer closed");
                }
            } catch (WakeupException e) {
                log.info("Kafka audit consumer wakeup during init — shutting down");
                return;
            } catch (Exception e) {
                log.error("Kafka audit consumer session failed, retrying in {}ms: {}",
                        retryDelayMs, e.getMessage(), e);
                kafkaConsumer = null;
                sleepBeforeRetry();
            }
        }
        log.info("Kafka audit consumer thread exiting");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processRecord(String json) {
        log.info("Kafka audit consumer: persisting record ({} bytes)", json.length());
        deserialize(json)
                .flatMap(auditLogUseCase::save)
                .doOnError(e -> log.error("Failed to persist audit log from Kafka: {}", e.getMessage()))
                .onErrorComplete()
                .subscribe();
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
