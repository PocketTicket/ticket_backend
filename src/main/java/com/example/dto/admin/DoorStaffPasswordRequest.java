package com.example.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoorStaffPasswordRequest(
        // bcrypt only uses the first 72 bytes of a password.
        @NotNull(message = "password is required")
        @Size(min = 8, max = 72, message = "password must have between 8 and 72 characters")
        String password
) { }
