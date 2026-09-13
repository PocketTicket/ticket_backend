package com.example.security;

import com.example.repository.AdminRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Makes sure the admin from .env (ADMIN_USERNAME, ADMIN_PASSWORD) can log in: it is
 * created on startup, and gets the new password if the one in .env was changed.
 */
@ApplicationScoped
public class ConfiguredAdmin {

    @ConfigProperty(name = "ticket.admin.username")
    String username;

    @ConfigProperty(name = "ticket.admin.password")
    String password;

    @Inject
    AdminRepository adminRepository;

    void onStart(@Observes StartupEvent event) {
        adminRepository.saveAdmin(username, BcryptUtil.bcryptHash(password));
    }
}
