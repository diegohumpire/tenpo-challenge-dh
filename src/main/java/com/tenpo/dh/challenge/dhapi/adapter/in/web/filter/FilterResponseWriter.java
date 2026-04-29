package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Utility component que escribe respuestas de error en formato
 * {@code application/problem+json} (RFC 7807) directamente en el response,
 * sin pasar por el {@code GlobalExceptionHandler}.
 * <p>
 * Se usa en filtros WebFlux donde el manejo de excepciones aún no está activo
 * y la respuesta de error debe generarse antes de llegar al handler.
 */
@Component
public class FilterResponseWriter {

    private static final String PROBLEM_TYPE = "about:blank";

    /**
     * Escribe una respuesta HTTP {@code 400 Bad Request} en formato problem+json.
     *
     * @param exchange el exchange del servidor
     * @param title    título del error (e.g. "Missing Required Headers")
     * @param detail   detalle del error con información específica
     */
    public Mono<Void> writeBadRequest(ServerWebExchange exchange, String title, String detail) {
        return writeError(exchange, HttpStatus.BAD_REQUEST, title, detail);
    }

    /**
     * Escribe una respuesta HTTP {@code 429 Too Many Requests} en formato problem+json.
     *
     * @param exchange el exchange del servidor
     * @param detail   detalle del error con información de rate limit
     */
    public Mono<Void> writeTooManyRequests(ServerWebExchange exchange, String detail) {
        return writeError(exchange, HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", detail);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status,
                                   String title, String detail) {
        String body = String.format(
                "{\"status\":%d,\"title\":\"%s\",\"detail\":\"%s\",\"type\":\"%s\"}",
                status.value(), title, detail, PROBLEM_TYPE);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
