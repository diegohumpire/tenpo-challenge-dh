package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Stores the current {@link ServerWebExchange} in the Reactor context so that
 * downstream components (e.g. {@code InMemoryPercentageProvider}) can read
 * request headers via {@code Mono.deferContextual}.
 *
 * <p>Must run before all other filters — hence {@link Ordered#HIGHEST_PRECEDENCE}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExchangeContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange));
    }
}
