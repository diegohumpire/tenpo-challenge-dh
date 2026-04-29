package com.tenpo.dh.challenge.dhapi.adapter.out.messaging;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.port.in.AuditLogUseCase;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * Async audit-event publisher backed by a Reactor unicast Sink.
 *
 * The filter calls {@link #publish} (non-blocking) and returns immediately.
 * A background subscriber drains the sink and persists each entry on a
 * bounded-elastic thread, so DB latency never blocks the HTTP response.
 *
 * To route events through Kafka instead, implement {@link AuditEventPublisher}
 * with a KafkaTemplate and register it as the primary bean.
 */
@Slf4j
@Component
public class SinkAuditEventPublisher implements AuditEventPublisher {

    private final Sinks.Many<AuditLog> sink;
    private final Disposable subscription;

    public SinkAuditEventPublisher(AuditLogUseCase auditLogUseCase) {
        this.sink = Sinks.many().unicast().onBackpressureBuffer();

        this.subscription = sink.asFlux()
                .flatMap(auditLog -> auditLogUseCase.save(auditLog)
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("Audit log save failed: {}", e.getMessage());
                            return Mono.empty();
                        }))
                .subscribe();
    }

    @Override
    public void publish(AuditLog auditLog) {
        Sinks.EmitResult result = sink.tryEmitNext(auditLog);
        if (result.isFailure()) {
            log.warn("Audit event dropped (sink full or closed): {}", result);
        }
    }

    @PreDestroy
    public void destroy() {
        sink.tryEmitComplete();
        subscription.dispose();
    }
}
