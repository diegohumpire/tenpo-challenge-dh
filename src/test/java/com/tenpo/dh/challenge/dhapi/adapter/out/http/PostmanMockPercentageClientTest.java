package com.tenpo.dh.challenge.dhapi.adapter.out.http;

import com.tenpo.dh.challenge.dhapi.config.PercentageProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PostmanMockPercentageClientTest {

    private PercentageProperties properties;
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        properties = new PercentageProperties();
        properties.getRetry().setMaxAttempts(0);
        properties.getRetry().setInitialBackoffSeconds(0);
        properties.getRetry().setMaxBackoffSeconds(0);
        circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("postman-test");
    }

    @Test
    void getServiceName_returnsPostmanMockPercentageService() {
        PostmanMockPercentageClient client = createClient(mockExchangeReturning("{\"percentage\":10.0}"));
        assertThat(client.getServiceName()).isEqualTo("PostmanMockPercentageService");
    }

    @Test
    void buildRequest_withoutContext_returnsRequestSpec() {
        PostmanMockPercentageClient client = createClient(mockExchangeReturning("{\"percentage\":10.0}"));

        StepVerifier.create(client.buildRequest())
                .assertNext(spec -> assertThat(spec).isNotNull())
                .verifyComplete();
    }

    @Test
    void buildRequest_withContextAndMockResponseCodeHeader_returnsRequestSpec() {
        PostmanMockPercentageClient client = createClient(mockExchangeReturning("{\"percentage\":10.0}"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test")
                        .header("x-mock-response-code", "500")
                        .build());

        StepVerifier.create(
                client.buildRequest()
                        .contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
                .assertNext(spec -> {
                    assertThat(spec).isNotNull();
                    assertThat(spec.requestHeadersSummary()).contains("x-mock-response-code=500");
                })
                .verifyComplete();
    }

    @Test
    void buildRequest_withContextAndMockPercentageHeader_returnsRequestSpec() {
        PostmanMockPercentageClient client = createClient(mockExchangeReturning("{\"percentage\":25.5}"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test")
                        .header("x-mock-percentage", "25.5")
                        .build());

        StepVerifier.create(
                client.buildRequest()
                        .contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
                .assertNext(spec -> {
                    assertThat(spec).isNotNull();
                    assertThat(spec.requestHeadersSummary()).contains("x-mock-percentage=25.5");
                })
                .verifyComplete();
    }

    @Test
    void buildRequest_withoutContext_hasNullRequestHeadersSummary() {
        PostmanMockPercentageClient client = createClient(mockExchangeReturning("{\"percentage\":10.0}"));

        StepVerifier.create(client.buildRequest())
                .assertNext(spec -> assertThat(spec.requestHeadersSummary()).isNull())
                .verifyComplete();
    }

    @Test
    void getPercentage_success_returnsExpectedValue() {
        PostmanMockPercentageClient client = createClient(mockExchangeReturning("{\"percentage\":25.0}"));

        StepVerifier.create(client.getPercentage())
                .assertNext(outcome -> {
                    assertThat(outcome.percentage()).isEqualByComparingTo("25.0");
                    assertThat(outcome.endpoint()).isEqualTo(
                            properties.getPostmanMock().getBaseUrl() + properties.getPostmanMock().getPath());
                    assertThat(outcome.requestHeaders()).isNull();
                    assertThat(outcome.responseHeaders()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void percentageResponse_createsAndExposesValue() {
        PercentageResponse response = new PercentageResponse(BigDecimal.TEN);
        assertThat(response.percentage()).isEqualByComparingTo("10");
    }

    private PostmanMockPercentageClient createClient(
            org.springframework.web.reactive.function.client.ExchangeFunction exchangeFunction) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        return new PostmanMockPercentageClient(builder, properties, circuitBreaker);
    }

    private org.springframework.web.reactive.function.client.ExchangeFunction mockExchangeReturning(
            String jsonBody) {
        return request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(jsonBody)
                        .build());
    }
}
