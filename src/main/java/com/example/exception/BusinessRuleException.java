package com.example.exception;

/**
 * The request was well-formed but conflicts with the current state,
 * e.g. not enough stock or an order that can no longer be cancelled.
 * Becomes HTTP 409.
 */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String message) {
        super(409, message);
    }
}
