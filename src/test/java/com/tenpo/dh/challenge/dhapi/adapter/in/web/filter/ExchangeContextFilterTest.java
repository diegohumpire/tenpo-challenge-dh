package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditRequestContext;
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

    @Test
    void filter_putsAuditRequestContextInReactorContext() {
        ExchangeContextFilter filter = new ExchangeContextFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-abc")
                        .header(RequestHeadersFilter.HEADER_USER_ID, "user-xyz")
                        .build());

        AtomicReference<AuditRequestContext> captured = new AtomicReference<>();
        WebFilterChain chain = ex -> Mono.deferContextual(ctx -> {
            if (ctx.hasKey(AuditRequestContext.class)) {
                captured.set(ctx.get(AuditRequestContext.class));
            }
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().transactionalId()).isEqualTo("txn-abc");
        assertThat(captured.get().userId()).isEqualTo("user-xyz");
    }

    @Test
    void filter_requestWithoutHeaders_putsAuditRequestContextWithNullValues() {
        ExchangeContextFilter filter = new ExchangeContextFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test").build());

        AtomicReference<AuditRequestContext> captured = new AtomicReference<>();
        WebFilterChain chain = ex -> Mono.deferContextual(ctx -> {
            captured.set(ctx.getOrDefault(AuditRequestContext.class, AuditRequestContext.empty()));
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().transactionalId()).isNull();
        assertThat(captured.get().userId()).isNull();
    }
}
