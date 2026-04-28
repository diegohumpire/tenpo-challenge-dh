package com.tenpo.dh.challenge.dhapi.domain.port.in;

import com.tenpo.dh.challenge.dhapi.domain.model.Calculation;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface CalculationUseCase {
    Mono<Calculation> calculate(BigDecimal num1, BigDecimal num2);
}
