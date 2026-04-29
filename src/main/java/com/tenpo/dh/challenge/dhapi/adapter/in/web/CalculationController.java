package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.CalculationRequest;
import com.tenpo.dh.challenge.dhapi.adapter.in.web.dto.CalculationResponse;
import com.tenpo.dh.challenge.dhapi.domain.port.in.CalculationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Operation(summary = "Perform a calculation with dynamic percentage", description = "Sums num1 and num2, then applies a percentage obtained from an external service", parameters = {
            @Parameter(in = ParameterIn.HEADER, name = "X-Transactional-Id", required = true, description = "Correlation ID for distributed tracing, injected by the API Gateway", example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.HEADER, name = "X-User-Id", required = true, description = "Authenticated user identity, injected by the API Gateway", example = "user-123", schema = @Schema(type = "string"))
    })
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Calculation successful", headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID for distributed tracing", schema = @Schema(type = "string", example = "550e8400-e29b-41d4-a716-446655440000")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            }),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters or missing required headers", headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID (present when X-Transactional-Id was supplied in the request)", schema = @Schema(type = "string")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            }),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded", headers = {
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "0")),
                    @Header(name = "X-RateLimit-Reset", description = "Unix timestamp (seconds) when the rate limit window resets", schema = @Schema(type = "integer", example = "1714000060")),
                    @Header(name = "Retry-After", description = "Seconds to wait before making another request", schema = @Schema(type = "integer", example = "58"))
            }),
            @ApiResponse(responseCode = "503", description = "External percentage service unavailable", headers = {
                    @Header(name = "X-Transactional-Id", description = "Echoed correlation ID for distributed tracing", schema = @Schema(type = "string", example = "550e8400-e29b-41d4-a716-446655440000")),
                    @Header(name = "X-RateLimit-Limit", description = "Maximum requests allowed per time window", schema = @Schema(type = "integer", example = "3")),
                    @Header(name = "X-RateLimit-Remaining", description = "Remaining requests in the current window", schema = @Schema(type = "integer", example = "2"))
            })
    })
    @PostMapping
    public Mono<ResponseEntity<CalculationResponse>> calculate(
            @Valid @RequestBody CalculationRequest request) {
        return calculationUseCase.calculate(request.num1(), request.num2())
                .map(CalculationResponse::from)
                .map(resp -> ResponseEntity.status(HttpStatus.CREATED).body(resp));
    }
}
