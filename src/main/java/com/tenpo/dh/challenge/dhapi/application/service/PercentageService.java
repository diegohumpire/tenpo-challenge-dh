package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.exception.PercentageNotAvailableException;
import com.tenpo.dh.challenge.dhapi.domain.port.in.PercentageResolverUseCase;
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

    private final PercentageProvider percentageProvider;
    private final PercentageCacheStore percentageCacheStore;

    /**
     * Resolves the percentage:
     * 1. Try external service (with built-in retries in the client)
     * 2. On success → cache the value and return it
     * 3. On failure → fall back to cached value
     * 4. No cached value → throw PercentageNotAvailableException (→ 503)
     */
    @Override
    public Mono<BigDecimal> resolvePercentage() {
        return percentageProvider.getPercentage()
                .flatMap(pct -> percentageCacheStore.put(pct).thenReturn(pct))
                .onErrorResume(e -> {
                    log.warn("External percentage service unavailable, falling back to cache. Reason: {}", e.getMessage());
                    return percentageCacheStore.get()
                            .switchIfEmpty(Mono.error(new PercentageNotAvailableException()));
                });
    }
}
