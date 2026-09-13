package com.example.security;

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

    public static final String ADMIN_ROLE = "admin";

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
            String hash = adminRepository.getPasswordHash(request.getUsername());
            String password = new String(request.getPassword().getPassword());

            if (hash == null || !BcryptUtil.matches(password, hash)) {
                throw new AuthenticationFailedException();
            }
            return adminIdentity(request.getUsername());
        });
    }

    static SecurityIdentity adminIdentity(String username) {
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(username))
                .addRole(ADMIN_ROLE)
                .build();
    }
}
