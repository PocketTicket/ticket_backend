package com.example.security;

import com.example.models.admin.AdminRole;
import com.example.repository.AdminRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * A fresh installation ships with two accounts for the login form:
 * <ul>
 *     <li>"admin" / "admin": the admin panel. Logged in with it, the admin can only set a
 *     personal password and an email address; the panel opens up once that is done.</li>
 *     <li>"einlass" / "einlass": shared by all door staff and allowed to do nothing but check
 *     tickets in, so its login may be known to every helper. The admin can change its password.</li>
 * </ul>
 * Each is created whenever no account of its role exists, so deleting it from the database
 * and restarting is also the way back in after a forgotten password.
 */
@ApplicationScoped
public class DefaultAccounts {

    @Inject
    AdminRepository adminRepository;

    void onStart(@Observes StartupEvent event) {
        createIfMissing(AdminRole.ADMIN, "admin", "admin");
        createIfMissing(AdminRole.DOOR_STAFF, "einlass", "einlass");
    }

    private void createIfMissing(AdminRole role, String username, String password) {
        if (!adminRepository.hasAccountWithRole(role)) {
            adminRepository.createAdmin(username, BcryptUtil.bcryptHash(password), role);
        }
    }
}
