package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Decorator de {@link ServerHttpRequest} que cachea el body en un byte array
 * para permitir múltiples lecturas del mismo Publisher.
 * <p>
 * Necesario en WebFlux porque el Publisher del body subyacente es unicast:
 * suscribirse más de una vez (en el filtro y en el handler) fallaría.
 * Este decorator almacena los bytes en construcción y los reproduce
 * en cada llamada a {@link #getBody()}.
 */
public class RequestBodyBufferingDecorator extends ServerHttpRequestDecorator {

    private final byte[] bodyBytes;
    private final DataBufferFactory bufferFactory;

    /**
     * @param delegate      el request original a decorar
     * @param bodyBytes     los bytes del body ya leídos
     * @param bufferFactory factory para crear {@link DataBuffer} en cada llamada a {@code getBody()}
     */
    public RequestBodyBufferingDecorator(ServerHttpRequest delegate,
                                          byte[] bodyBytes,
                                          DataBufferFactory bufferFactory) {
        super(delegate);
        this.bodyBytes = bodyBytes;
        this.bufferFactory = bufferFactory;
    }

    /** Retorna un Flux con los bytes cacheados, reproducible en múltiples suscripciones. */
    @Override
    public Flux<DataBuffer> getBody() {
        return Mono.fromSupplier(() -> bufferFactory.wrap(bodyBytes)).flux();
    }
}
