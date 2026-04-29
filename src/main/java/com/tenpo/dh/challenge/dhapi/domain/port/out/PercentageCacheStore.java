package com.tenpo.dh.challenge.dhapi.domain.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PercentageCacheStore {
    Mono<BigDecimal> get();
    Mono<Void> put(BigDecimal percentage);
    Mono<Void> clear();
}
