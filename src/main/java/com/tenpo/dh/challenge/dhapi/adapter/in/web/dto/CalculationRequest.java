package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request body for the calculation endpoint")
public record CalculationRequest(
        @NotNull(message = "num1 is required")
        @Schema(description = "First number", example = "5.0")
        BigDecimal num1,

        @NotNull(message = "num2 is required")
        @Schema(description = "Second number", example = "5.0")
        BigDecimal num2
) {}
