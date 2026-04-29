package com.tenpo.dh.challenge.dhapi.domain.port.out;

import com.tenpo.dh.challenge.dhapi.domain.model.PercentageCallOutcome;
import reactor.core.publisher.Mono;

public interface PercentageProvider {
    Mono<PercentageCallOutcome> getPercentage();
}
