package com.example.dto.error;

public record ErrorResponse(
        int status,
        String message
) { }
