package com.tenpo.dh.challenge.dhapi.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Calculation(
        BigDecimal num1,
        BigDecimal num2,
        BigDecimal sum,
        BigDecimal percentage,
        BigDecimal result
) {
    public static Calculation of(BigDecimal num1, BigDecimal num2, BigDecimal percentage) {
        BigDecimal sum = num1.add(num2);
        BigDecimal increase = sum.multiply(percentage).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal result = sum.add(increase).stripTrailingZeros();
        return new Calculation(num1, num2, sum, percentage, result);
    }
}
