package com.example.security;

import com.example.repository.AdminRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A fresh installation ships with the admin account "admin" / "admin". Logged in with
 * it, the admin can only set a personal password and a contact email address; the admin
 * panel opens up once that is done (see {@link AdminPasswordProvider#adminIdentity}).
 *
 * <p>The account is created whenever no admin exists, so deleting all admins from the
 * database and restarting is also the way back in after a forgotten password.
 */
@ApplicationScoped
public class DefaultAdmin {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    @Inject
    AdminRepository adminRepository;

    void onStart(@Observes StartupEvent event) {
        if (!adminRepository.hasAdmins()) {
            adminRepository.createAdmin(USERNAME, BcryptUtil.bcryptHash(PASSWORD));
        }
    }
}
