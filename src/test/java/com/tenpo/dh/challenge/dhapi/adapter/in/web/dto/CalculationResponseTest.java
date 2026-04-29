package com.tenpo.dh.challenge.dhapi.adapter.in.web.dto;

import com.tenpo.dh.challenge.dhapi.domain.model.Calculation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationResponseTest {

    @Test
    void from_mapsAllCalculationFieldsCorrectly() {
        Calculation calc = Calculation.of(
                BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(10));

        CalculationResponse response = CalculationResponse.from(calc);

        assertThat(response.num1()).isEqualByComparingTo("5");
        assertThat(response.num2()).isEqualByComparingTo("5");
        assertThat(response.sum()).isEqualByComparingTo("10");
        assertThat(response.percentage()).isEqualByComparingTo("10");
        assertThat(response.result()).isEqualByComparingTo("11.0");
    }
}
