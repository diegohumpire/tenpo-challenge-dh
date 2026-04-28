package com.tenpo.dh.challenge.dhapi.adapter.in.web.exception;

import com.tenpo.dh.challenge.dhapi.domain.exception.PercentageNotAvailableException;
import com.tenpo.dh.challenge.dhapi.domain.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handlePercentageNotAvailable_returns503() {
        ResponseEntity<ProblemDetail> resp = handler.handlePercentageNotAvailable(
                new PercentageNotAvailableException());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(503);
        assertThat(resp.getBody().getTitle()).isEqualTo("Service Unavailable");
    }

    @Test
    void handleRateLimit_returns429() {
        ResponseEntity<ProblemDetail> resp = handler.handleRateLimit(
                new RateLimitExceededException());

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(429);
        assertThat(resp.getBody().getTitle()).isEqualTo("Too Many Requests");
    }

    @Test
    void handleGeneral_returns500() {
        ResponseEntity<ProblemDetail> resp = handler.handleGeneral(
                new RuntimeException("Unexpected"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(500);
    }

    @Test
    void handleResponseStatus_returnsCorrectStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        ResponseEntity<ProblemDetail> resp = handler.handleResponseStatus(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(404);
    }

    @Test
    void handleServerWebInput_returns400() {
        var ex = new org.springframework.web.server.ServerWebInputException("Bad body");
        ResponseEntity<ProblemDetail> resp = handler.handleServerWebInput(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getStatus()).isEqualTo(400);
    }
}
