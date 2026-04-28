package com.tenpo.dh.challenge.dhapi.domain.port.out;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface PercentageProvider {
    Mono<BigDecimal> getPercentage();
}
