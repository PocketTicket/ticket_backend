package com.example.exception;

/**
 * The request is well-formed, but its content cannot be accepted, e.g. a wrong current
 * password. Becomes HTTP 400.
 */
public class InvalidRequestException extends ApiException {
    public InvalidRequestException(String message) {
        super(400, message);
    }
}
