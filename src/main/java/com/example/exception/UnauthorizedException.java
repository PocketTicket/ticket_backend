package com.example.exception;

/** The request needs a logged-in user, but nobody is logged in. Becomes HTTP 401. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(401, message);
    }
}
