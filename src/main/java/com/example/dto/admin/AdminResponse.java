package com.example.dto.admin;

public record AdminResponse(
        String username,
        String email,
        // True until the admin set a personal password and an email address with
        // PUT /admin/account. Until then the admin panel answers 403.
        boolean setupRequired
) { }
