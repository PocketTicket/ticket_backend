package com.example.exception;

/** Nothing exists under the requested id. Becomes HTTP 404. */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}
