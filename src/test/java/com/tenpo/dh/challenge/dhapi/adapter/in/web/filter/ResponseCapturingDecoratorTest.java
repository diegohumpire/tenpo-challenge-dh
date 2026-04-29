package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseCapturingDecoratorTest {

    @Test
    void constructor_registersTransactionalIdViaBeforeCommit() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-abc")
                        .build());

        AtomicReference<String> bodyRef = new AtomicReference<>("");
        ResponseCapturingDecorator decorator = new ResponseCapturingDecorator(
                exchange.getResponse(), bodyRef, "txn-abc", new BodyCompactor());

        // Escribir algo para activar el commit (y beforeCommit)
        byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
        StepVerifier.create(decorator.writeWith(
                        Mono.fromSupplier(() -> exchange.getResponse().bufferFactory().wrap(bytes))))
                .verifyComplete();

        assertThat(decorator.getHeaders().getFirst(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID))
                .isEqualTo("txn-abc");
    }

    @Test
    void writeWith_capturesAndCompactsResponseBody() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations").build());

        AtomicReference<String> bodyRef = new AtomicReference<>("");
        ResponseCapturingDecorator decorator = new ResponseCapturingDecorator(
                exchange.getResponse(), bodyRef, "txn-test", new BodyCompactor());

        String prettyJson = "{\n  \"result\": 11.0\n}";
        byte[] bytes = prettyJson.getBytes(StandardCharsets.UTF_8);

        StepVerifier.create(decorator.writeWith(
                        Mono.fromSupplier(() -> exchange.getResponse().bufferFactory().wrap(bytes))))
                .verifyComplete();

        assertThat(bodyRef.get()).isEqualTo("{\"result\":11.0}");
    }
}
