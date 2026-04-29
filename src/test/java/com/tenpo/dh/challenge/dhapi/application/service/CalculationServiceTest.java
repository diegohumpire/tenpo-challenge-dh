package com.tenpo.dh.challenge.dhapi.application.service;

import com.tenpo.dh.challenge.dhapi.domain.model.Calculation;
import com.tenpo.dh.challenge.dhapi.domain.port.in.PercentageResolverUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock
    private PercentageResolverUseCase percentageResolverUseCase;

    @InjectMocks
    private CalculationService calculationService;

    @Test
    void calculate_withValidNumbers_returnsCorrectResult() {
        when(percentageResolverUseCase.resolvePercentage()).thenReturn(Mono.just(BigDecimal.valueOf(10)));

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
                .assertNext(calc -> {
                    assertThat(calc.sum()).isEqualByComparingTo("10.0");
                    assertThat(calc.percentage()).isEqualByComparingTo("10");
                    assertThat(calc.result()).isEqualByComparingTo("11.0");
                })
                .verifyComplete();
    }

    @Test
    void calculate_withZeroPercentage_returnsSumOnly() {
        when(percentageResolverUseCase.resolvePercentage()).thenReturn(Mono.just(BigDecimal.ZERO));

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(3), BigDecimal.valueOf(7)))
                .assertNext(calc -> {
                    assertThat(calc.sum()).isEqualByComparingTo("10.0");
                    assertThat(calc.result()).isEqualByComparingTo("10.0");
                })
                .verifyComplete();
    }

    @Test
    void calculate_withNegativeNumbers_computesCorrectly() {
        when(percentageResolverUseCase.resolvePercentage()).thenReturn(Mono.just(BigDecimal.valueOf(10)));

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(-5), BigDecimal.valueOf(15)))
                .assertNext(calc -> {
                    assertThat(calc.sum()).isEqualByComparingTo("10.0");
                    assertThat(calc.result()).isEqualByComparingTo("11.0");
                })
                .verifyComplete();
    }

    @Test
    void calculate_propagatesErrorFromPercentageService() {
        when(percentageResolverUseCase.resolvePercentage())
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
                .expectErrorMessage("Service unavailable")
                .verify();
    }

    @Test
    void calculation_of_computesResultCorrectly() {
        Calculation calc = Calculation.of(
                BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(10));

        assertThat(calc.num1()).isEqualByComparingTo("5");
        assertThat(calc.num2()).isEqualByComparingTo("5");
        assertThat(calc.sum()).isEqualByComparingTo("10");
        assertThat(calc.percentage()).isEqualByComparingTo("10");
        assertThat(calc.result()).isEqualByComparingTo("11.0");
    }
}
