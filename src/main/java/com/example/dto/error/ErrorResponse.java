package com.example.dto.error;

// INFO - outbound only. Uniform error body for every handled ApiException.
public record ErrorResponse(
        int status,
        String message
) { }
