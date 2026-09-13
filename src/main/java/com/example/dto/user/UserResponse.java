package com.example.dto.user;

import com.example.models.user.SsoProvider;

public record UserResponse(
        String firstName,
        String lastName,
        String email,
        SsoProvider provider
) { }
