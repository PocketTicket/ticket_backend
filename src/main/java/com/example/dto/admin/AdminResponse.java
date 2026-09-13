package com.example.dto.admin;

import com.example.models.admin.AdminRole;

public record AdminResponse(
        String username,
        // DOOR_STAFF can only check tickets in
        AdminRole role,
        String email,
        // True until an admin set a personal password and an email address with
        // PUT /admin/account. Until then the admin panel answers 403. Always false for door staff.
        boolean setupRequired
) { }
