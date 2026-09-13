package com.example.security;

import com.example.models.admin.Admin;
import com.example.repository.AdminRepository;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Accepts the login cookie on every request after the admin logged in. The cookie is
 * encrypted with SESSION_ENCRYPTION_KEY and only issued after a correct password, so
 * the username inside it can be trusted.
 */
@ApplicationScoped
public class AdminSessionProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    @Inject
    AdminRepository adminRepository;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
                                              AuthenticationRequestContext context) {
        // The admin is read again on every request, so finishing the setup opens the
        // admin panel right away, without logging in again.
        return context.runBlocking(() -> {
            Admin admin = adminRepository.getAdminByUsername(request.getPrincipal());

            if (admin == null) {
                throw new AuthenticationFailedException();
            }
            return AdminPasswordProvider.adminIdentity(admin);
        });
    }
}
