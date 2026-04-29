package com.tenpo.dh.challenge.dhapi.domain.port.in;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PercentageResolverUseCase {
    Mono<BigDecimal> resolvePercentage();
}
