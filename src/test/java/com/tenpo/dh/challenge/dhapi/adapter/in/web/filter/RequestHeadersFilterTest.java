package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestHeadersFilterTest {

    @Mock
    private WebFilterChain chain;

    private RequestHeadersFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RequestHeadersFilter();
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_bothHeadersPresent_proceedsToChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-abc")
                        .header(RequestHeadersFilter.HEADER_USER_ID, "user-42")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void filter_missingTransactionalId_returns400() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_USER_ID, "user-42")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(chain);
    }

    @Test
    void filter_missingUserId_returns400() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "txn-abc")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(chain);
    }

    @Test
    void filter_bothHeadersMissing_returns400() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(chain);
    }

    @Test
    void filter_blankTransactionalId_returns400() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/calculations")
                        .header(RequestHeadersFilter.HEADER_TRANSACTIONAL_ID, "   ")
                        .header(RequestHeadersFilter.HEADER_USER_ID, "user-42")
                        .build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void filter_excludedActuatorPath_skipsValidation() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void filter_excludedSwaggerPath_skipsValidation() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/swagger-ui/index.html").build());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }
}
