package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RequestBodyBufferingDecoratorTest {

    @Test
    void getBody_returnsBufferedBytesOnEverySubscription() {
        byte[] bodyBytes = "{\"num1\":5.0}".getBytes(StandardCharsets.UTF_8);
        var factory = new DefaultDataBufferFactory();

        RequestBodyBufferingDecorator decorator = new RequestBodyBufferingDecorator(
                MockServerHttpRequest.post("/test").build(),
                bodyBytes,
                factory);

        // Primera suscripción
        StepVerifier.create(decorator.getBody())
                .assertNext(buffer -> {
                    byte[] read = new byte[buffer.readableByteCount()];
                    buffer.read(read);
                    assertThat(new String(read, StandardCharsets.UTF_8)).isEqualTo("{\"num1\":5.0}");
                })
                .verifyComplete();

        // Segunda suscripción — debe funcionar (reproducible)
        StepVerifier.create(decorator.getBody())
                .assertNext(buffer -> {
                    byte[] read = new byte[buffer.readableByteCount()];
                    buffer.read(read);
                    assertThat(new String(read, StandardCharsets.UTF_8)).isEqualTo("{\"num1\":5.0}");
                })
                .verifyComplete();
    }

    @Test
    void getBody_withEmptyBytes_returnsEmptyBuffer() {
        var factory = new DefaultDataBufferFactory();

        RequestBodyBufferingDecorator decorator = new RequestBodyBufferingDecorator(
                MockServerHttpRequest.get("/test").build(),
                new byte[0],
                factory);

        StepVerifier.create(decorator.getBody())
                .assertNext(buffer -> assertThat(buffer.readableByteCount()).isZero())
                .verifyComplete();
    }
}
