package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.tenpo.dh.challenge.dhapi.domain.model.AuditRequestContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Stores the current {@link ServerWebExchange} and an {@link AuditRequestContext}
 * in the Reactor context so that downstream components can read request data
 * without coupling to the web layer.
 *
 * <p>Must run before all other filters — hence {@link Ordered#HIGHEST_PRECEDENCE}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExchangeContextFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        AuditRequestContext auditCtx = new AuditRequestContext(
                exchange.getRequest().getHeaders().getFirst(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID),
                exchange.getRequest().getHeaders().getFirst(RequestHeadersFilter.HEADER_USER_ID));

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx
                        .put(ServerWebExchange.class, exchange)
                        .put(AuditRequestContext.class, auditCtx));
    }
}
