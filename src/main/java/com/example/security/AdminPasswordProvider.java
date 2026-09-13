package com.example.security;

import com.example.models.admin.Admin;
import com.example.repository.AdminRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Checks username and password when an admin logs in on the admin login form
 * (POST /admin/login, see application.properties). Afterwards Quarkus keeps the admin
 * logged in with an encrypted cookie, which {@link AdminSessionProvider} accepts.
 */
@ApplicationScoped
public class AdminPasswordProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    /** Access to the admin panel. Only given once the admin finished the first-login setup. */
    public static final String ADMIN_ROLE = "admin";

    /** Access to the own account (see AdminController). Given to every logged-in admin. */
    public static final String ACCOUNT_ROLE = "admin-account";

    @Inject
    AdminRepository adminRepository;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
                                              AuthenticationRequestContext context) {
        // runBlocking, because jOOQ talks to the database with blocking JDBC.
        return context.runBlocking(() -> {
            Admin admin = adminRepository.getAdminByUsername(request.getUsername());
            String password = new String(request.getPassword().getPassword());

            if (admin == null || !BcryptUtil.matches(password, admin.passwordHash())) {
                throw new AuthenticationFailedException();
            }
            return adminIdentity(admin);
        });
    }

    static SecurityIdentity adminIdentity(Admin admin) {
        QuarkusSecurityIdentity.Builder identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(admin.username()))
                .addRole(ACCOUNT_ROLE);

        if (!admin.setupRequired()) {
            identity.addRole(ADMIN_ROLE);
        }
        return identity.build();
    }
}
