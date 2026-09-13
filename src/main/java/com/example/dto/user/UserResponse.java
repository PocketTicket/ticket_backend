package com.example.dto.user;

import com.example.models.user.AuthProvider;
import com.example.models.user.UserRole;

import java.time.LocalDateTime;

/** Carries no password hash - that value never leaves the service layer. */
public record UserResponse(
        int userId,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        AuthProvider authProvider,
        LocalDateTime createdAt
) { }
