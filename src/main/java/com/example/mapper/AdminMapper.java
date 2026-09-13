package com.example.mapper;

import com.example.dto.admin.AdminResponse;
import com.example.models.admin.Admin;

public final class AdminMapper {

    private AdminMapper() {
    }

    /** The password hash is left out on purpose. */
    public static AdminResponse toResponse(Admin admin) {
        return new AdminResponse(admin.username(), admin.role(), admin.email(), admin.setupRequired());
    }
}
