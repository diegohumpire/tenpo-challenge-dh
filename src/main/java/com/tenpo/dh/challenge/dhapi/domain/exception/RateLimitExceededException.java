package com.tenpo.dh.challenge.dhapi.domain.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Ha excedido el límite de solicitudes por minuto. Intente nuevamente más tarde.");
    }

    public RateLimitExceededException(String message) {
        super(message);
    }
}
