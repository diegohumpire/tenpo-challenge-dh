package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.CalculationRequest;
import com.tenpo.dh.challenge.dhapi.domain.model.Calculation;
import com.tenpo.dh.challenge.dhapi.domain.port.in.CalculationUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculationControllerTest {

    @Mock
    private CalculationUseCase calculationUseCase;

    @InjectMocks
    private CalculationController controller;

    @Test
    void calculate_returnsCreatedStatusWithBody() {
        Calculation calc = Calculation.of(
                BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(10));
        when(calculationUseCase.calculate(any(), any())).thenReturn(Mono.just(calc));

        CalculationRequest request = new CalculationRequest(
                BigDecimal.valueOf(5), BigDecimal.valueOf(5));

        StepVerifier.create(controller.calculate(request))
                .assertNext(resp -> {
                    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(resp.getBody()).isNotNull();
                    assertThat(resp.getBody().result()).isEqualByComparingTo("11.0");
                })
                .verifyComplete();
    }

    @Test
    void calculate_propagatesErrorFromUseCase() {
        when(calculationUseCase.calculate(any(), any()))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        CalculationRequest request = new CalculationRequest(
                BigDecimal.valueOf(5), BigDecimal.valueOf(5));

        StepVerifier.create(controller.calculate(request))
                .expectErrorMessage("Service unavailable")
                .verify();
    }
}
