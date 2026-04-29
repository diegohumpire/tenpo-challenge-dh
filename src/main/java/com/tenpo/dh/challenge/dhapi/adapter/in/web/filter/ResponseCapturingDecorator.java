package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Decorator de {@link ServerHttpResponse} que:
 * <ol>
 *   <li>Captura el response body al momento de escritura,
 *       almacenándolo compactado en un {@link AtomicReference}.</li>
 *   <li>Inyecta el header {@code X-Transactional-Id} en la respuesta via
 *       {@code beforeCommit()}, garantizando que esté presente antes de
 *       que los headers sean enviados al cliente.</li>
 * </ol>
 */
public class ResponseCapturingDecorator extends ServerHttpResponseDecorator {

    private final AtomicReference<String> responseBodyRef;
    private final BodyCompactor bodyCompactor;

    /**
     * @param delegate        el response original a decorar
     * @param responseBodyRef referencia donde se almacenará el body capturado
     * @param transactionalId valor a inyectar en {@code X-Transactional-Id}
     * @param bodyCompactor   componente para compactar el body capturado
     */
    public ResponseCapturingDecorator(ServerHttpResponse delegate,
                                       AtomicReference<String> responseBodyRef,
                                       String transactionalId,
                                       BodyCompactor bodyCompactor) {
        super(delegate);
        this.responseBodyRef = responseBodyRef;
        this.bodyCompactor = bodyCompactor;

        // Inyecta X-Transactional-Id antes de que los headers sean confirmados (committed).
        beforeCommit(() -> {
            getHeaders().set(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, transactionalId);
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        return DataBufferUtils.join(Flux.from(body))
                .flatMap(buffer -> {
                    byte[] respBytes = new byte[buffer.readableByteCount()];
                    buffer.read(respBytes);
                    DataBufferUtils.release(buffer);
                    responseBodyRef.set(bodyCompactor.compact(
                            new String(respBytes, StandardCharsets.UTF_8)));
                    return super.writeWith(
                            Mono.fromSupplier(() -> bufferFactory().wrap(respBytes)));
                })
                .switchIfEmpty(super.writeWith(Flux.empty()));
    }
}
