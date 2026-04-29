package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.exception.PercentageNotAvailableException;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditActionType;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditLog;
import com.tenpo.dh.challenge.dhapi.domain.model.AuditRequestContext;
import com.tenpo.dh.challenge.dhapi.domain.model.CallDirection;
import com.tenpo.dh.challenge.dhapi.domain.model.PercentageCallOutcome;
import com.tenpo.dh.challenge.dhapi.domain.port.in.PercentageResolverUseCase;
import com.tenpo.dh.challenge.dhapi.domain.port.out.AuditEventPublisher;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageCacheStore;
import com.tenpo.dh.challenge.dhapi.domain.port.out.PercentageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PercentageService implements PercentageResolverUseCase {

    private static final String CACHE_ENDPOINT = "cache:percentage:current";

    private final PercentageProvider percentageProvider;
    private final PercentageCacheStore percentageCacheStore;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Resolves the percentage:
     * 1. Try external service (with built-in retries in the client)
     * 2. On success → cache the value and return it
     * 3. On failure → fall back to cached value
     * 4. No cached value → throw PercentageNotAvailableException (→ 503)
     *
     * <p>Each step (external call, cache read, cache write) is recorded as an audit event.
     */
    @Override
    public Mono<BigDecimal> resolvePercentage() {
        return Mono.deferContextual(ctx -> {
            AuditRequestContext auditCtx = ctx.getOrDefault(AuditRequestContext.class, AuditRequestContext.empty());
            long start = System.currentTimeMillis();

            return percentageProvider.getPercentage()
                    .flatMap(outcome -> {
                        publishExternalSuccess(auditCtx, outcome, System.currentTimeMillis() - start);
                        return percentageCacheStore.put(outcome.percentage())
                                .doOnSuccess(__ -> publishCachePut(auditCtx, outcome.percentage()))
                                .thenReturn(outcome.percentage());
                    })
                    .onErrorResume(e -> {
                        log.warn("External percentage service unavailable, falling back to cache. Reason: {}", e.getMessage());
                        publishExternalFailure(auditCtx, e, System.currentTimeMillis() - start);
                        return percentageCacheStore.get()
                                .doOnNext(pct -> publishCacheGetHit(auditCtx, pct))
                                .switchIfEmpty(Mono.defer(() -> {
                                    publishCacheGetMiss(auditCtx);
                                    return Mono.error(new PercentageNotAvailableException());
                                }));
                    });
        });
    }

    private void publishExternalSuccess(AuditRequestContext ctx, PercentageCallOutcome outcome, long durationMs) {
        auditEventPublisher.publish(AuditLog.builder()
                .action("GET_EXTERNAL_PERCENTAGE")
                .actionType(AuditActionType.EXTERNAL_CALL)
                .callDirection(CallDirection.OUT)
                .transactionalId(ctx.transactionalId())
                .userId(ctx.userId())
                .endpoint(outcome.endpoint())
                .requestHeaders(outcome.requestHeaders())
                .responseHeaders(outcome.responseHeaders())
                .responseBody("{\"percentage\":\"" + outcome.percentage().toPlainString() + "\"}")
                .statusCode(200)
                .durationMs(durationMs)
                .build());
    }

    private void publishExternalFailure(AuditRequestContext ctx, Throwable e, long durationMs) {
        auditEventPublisher.publish(AuditLog.builder()
                .action("GET_EXTERNAL_PERCENTAGE")
                .actionType(AuditActionType.EXTERNAL_CALL)
                .callDirection(CallDirection.OUT)
                .transactionalId(ctx.transactionalId())
                .userId(ctx.userId())
                .errorMessage(e.getMessage())
                .durationMs(durationMs)
                .build());
    }

    private void publishCachePut(AuditRequestContext ctx, BigDecimal pct) {
        auditEventPublisher.publish(AuditLog.builder()
                .action("CACHE_PUT_PERCENTAGE")
                .actionType(AuditActionType.CACHE_ACCESS)
                .callDirection(CallDirection.OUT)
                .transactionalId(ctx.transactionalId())
                .userId(ctx.userId())
                .method("PUT")
                .endpoint(CACHE_ENDPOINT)
                .requestBody("{\"percentage\":\"" + pct.toPlainString() + "\"}")
                .build());
    }

    private void publishCacheGetHit(AuditRequestContext ctx, BigDecimal pct) {
        auditEventPublisher.publish(AuditLog.builder()
                .action("CACHE_GET_PERCENTAGE")
                .actionType(AuditActionType.CACHE_ACCESS)
                .callDirection(CallDirection.OUT)
                .transactionalId(ctx.transactionalId())
                .userId(ctx.userId())
                .method("GET")
                .endpoint(CACHE_ENDPOINT)
                .responseBody("{\"percentage\":\"" + pct.toPlainString() + "\"}")
                .statusCode(200)
                .build());
    }

    private void publishCacheGetMiss(AuditRequestContext ctx) {
        auditEventPublisher.publish(AuditLog.builder()
                .action("CACHE_GET_PERCENTAGE")
                .actionType(AuditActionType.CACHE_ACCESS)
                .callDirection(CallDirection.OUT)
                .transactionalId(ctx.transactionalId())
                .userId(ctx.userId())
                .method("GET")
                .endpoint(CACHE_ENDPOINT)
                .statusCode(404)
                .build());
    }
}
