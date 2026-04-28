package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.CalculationRequest;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.CalculationResponse;
import com.tenpo.dh.challenge.dhapi.domain.port.in.CalculationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/calculations", version = "1")
@RequiredArgsConstructor
@Tag(name = "Calculations", description = "Endpoint for performing calculations with dynamic percentage")
public class CalculationController {

    private final CalculationUseCase calculationUseCase;

    @Operation(summary = "Perform a calculation with dynamic percentage", description = "Sums num1 and num2, then applies a percentage obtained from an external service")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Calculation successful"),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
            @ApiResponse(responseCode = "503", description = "External percentage service unavailable")
    })
    @PostMapping
    public Mono<ResponseEntity<CalculationResponse>> calculate(
            @Valid @RequestBody CalculationRequest request) {
        return calculationUseCase.calculate(request.num1(), request.num2())
                .map(CalculationResponse::from)
                .map(resp -> ResponseEntity.status(HttpStatus.CREATED).body(resp));
    }
}
