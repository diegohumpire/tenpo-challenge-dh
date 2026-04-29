package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ExchangeContextFilterTest {

    @Test
    void filter_putsExchangeInReactorContext() {
        ExchangeContextFilter filter = new ExchangeContextFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build());

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        WebFilterChain chain = ex -> Mono.deferContextual(ctx -> {
            if (ctx.hasKey(ServerWebExchange.class)) {
                capturedExchange.set(ctx.get(ServerWebExchange.class));
            }
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(capturedExchange.get()).isSameAs(exchange);
    }
}
