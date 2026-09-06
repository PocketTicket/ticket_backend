package com.example.exception;

/**
 * Base for errors the service layer raises deliberately. Carries the HTTP status
 * so the service never has to import anything from jakarta.ws.rs itself -
 * translating this into a response is the job of {@link ApiExceptionMapper}.
 */
public abstract class ApiException extends RuntimeException {
    private final int status;

    protected ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
