package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.model.Calculation;
import com.tenpo.dh.challenge.dhapi.domain.port.in.CalculationUseCase;
import com.tenpo.dh.challenge.dhapi.domain.port.in.PercentageResolverUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CalculationService implements CalculationUseCase {

    private final PercentageResolverUseCase percentageResolverUseCase;

    @Override
    public Mono<Calculation> calculate(BigDecimal num1, BigDecimal num2) {
        return percentageResolverUseCase.resolvePercentage()
                .map(pct -> Calculation.of(num1, num2, pct));
    }
}
