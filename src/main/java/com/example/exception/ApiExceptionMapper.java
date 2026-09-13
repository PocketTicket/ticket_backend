package com.example.exception;

import com.example.dto.error.ErrorResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        // The type is set explicitly, because the failing endpoint may produce something
        // else than JSON, e.g. GET /products/{productId}/image.
        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(exception.getStatus(), exception.getMessage()))
                .build();
    }
}
