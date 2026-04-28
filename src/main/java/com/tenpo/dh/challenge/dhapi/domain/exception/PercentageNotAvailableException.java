package com.tenpo.dh.challenge.dhapi.domain.exception;

public class PercentageNotAvailableException extends RuntimeException {
    public PercentageNotAvailableException() {
        super("El servicio de porcentaje no está disponible y no hay valor en caché");
    }

    public PercentageNotAvailableException(String message) {
        super(message);
    }

    public PercentageNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
