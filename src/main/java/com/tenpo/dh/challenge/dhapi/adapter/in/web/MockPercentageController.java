package com.tenpo.dh.challenge.dhapi.adapter.in.web;

import com.tenpo.dh.challenge.dhapi.adapter.out.http.PercentageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/mock")
public class MockPercentageController {

    @Value("${mock.percentage.value:10.0}")
    private BigDecimal percentageValue;

    @Value("${mock.percentage.fail:false}")
    private boolean fail;

    @GetMapping("/percentage")
    public Mono<PercentageResponse> getPercentage() {
        if (fail) {
            return Mono.error(new RuntimeException("Mock service failure"));
        }
        return Mono.just(new PercentageResponse(percentageValue));
    }
}
