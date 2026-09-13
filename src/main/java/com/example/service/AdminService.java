package com.example.service;

import com.example.dto.admin.AdminAccountRequest;
import com.example.dto.admin.AdminResponse;
import com.example.dto.admin.DoorStaffPasswordRequest;
import com.example.exception.InvalidRequestException;
import com.example.mapper.AdminMapper;
import com.example.models.admin.Admin;
import com.example.repository.AdminRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class AdminService {
    @Inject
    AdminRepository adminRepository;

    public AdminResponse getAdmin(String username) {
        return AdminMapper.toResponse(adminRepository.getAdminByUsername(username));
    }

    /**
     * Sets a new password and the contact email address. Required once after the first
     * login with the default password; afterwards it changes both.
     *
     * @throws InvalidRequestException if the current password is wrong.
     */
    public AdminResponse updateAccount(String username, AdminAccountRequest request) {
        Admin admin = adminRepository.getAdminByUsername(username);

        if (!BcryptUtil.matches(request.currentPassword(), admin.passwordHash())) {
            throw new InvalidRequestException("The current password is not correct");
        }

        adminRepository.updateAccount(username, BcryptUtil.bcryptHash(request.newPassword()), request.email());
        return getAdmin(username);
    }

    /** Sets the password of the account all door staff share. */
    public void updateDoorStaffPassword(DoorStaffPasswordRequest request) {
        adminRepository.updateDoorStaffPassword(BcryptUtil.bcryptHash(request.password()));
    }
}
