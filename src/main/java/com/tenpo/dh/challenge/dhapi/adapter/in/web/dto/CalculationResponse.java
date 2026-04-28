package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.tenpo.dh.challenge.dhapi.domain.model.Calculation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Result of a calculation with applied percentage")
public record CalculationResponse(
        @Schema(example = "5.0") BigDecimal num1,
        @Schema(example = "5.0") BigDecimal num2,
        @Schema(example = "10.0") BigDecimal sum,
        @Schema(example = "10.0") BigDecimal percentage,
        @Schema(example = "11.0") BigDecimal result
) {
    public static CalculationResponse from(Calculation c) {
        return new CalculationResponse(c.num1(), c.num2(), c.sum(), c.percentage(), c.result());
    }
}
