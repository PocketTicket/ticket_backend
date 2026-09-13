package com.example.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminAccountRequest(
        @NotBlank(message = "currentPassword must not be blank")
        String currentPassword,

        // bcrypt only uses the first 72 bytes of a password.
        @NotNull(message = "newPassword is required")
        @Size(min = 8, max = 72, message = "newPassword must have between 8 and 72 characters")
        String newPassword,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid address")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email
) { }
