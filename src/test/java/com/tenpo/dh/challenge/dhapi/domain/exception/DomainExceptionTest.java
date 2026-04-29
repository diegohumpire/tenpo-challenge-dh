package com.tenpo.dh.challenge.dhapi.domain.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    @Test
    void percentageNotAvailableException_defaultConstructor_hasExpectedMessage() {
        PercentageNotAvailableException ex = new PercentageNotAvailableException();
        assertThat(ex.getMessage()).contains("porcentaje");
    }

    @Test
    void percentageNotAvailableException_messageConstructor_usesProvidedMessage() {
        PercentageNotAvailableException ex = new PercentageNotAvailableException("custom message");
        assertThat(ex.getMessage()).isEqualTo("custom message");
    }

    @Test
    void percentageNotAvailableException_messageCauseConstructor_usesProvidedMessageAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        PercentageNotAvailableException ex = new PercentageNotAvailableException("custom", cause);
        assertThat(ex.getMessage()).isEqualTo("custom");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void rateLimitExceededException_defaultConstructor_hasExpectedMessage() {
        RateLimitExceededException ex = new RateLimitExceededException();
        assertThat(ex.getMessage()).contains("límite");
    }

    @Test
    void rateLimitExceededException_messageConstructor_usesProvidedMessage() {
        RateLimitExceededException ex = new RateLimitExceededException("too fast");
        assertThat(ex.getMessage()).isEqualTo("too fast");
    }
}
